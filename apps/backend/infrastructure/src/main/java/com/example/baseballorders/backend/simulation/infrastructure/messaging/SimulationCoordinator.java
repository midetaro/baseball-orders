package com.example.baseballorders.backend.simulation.infrastructure.messaging;

import com.example.baseballorders.backend.simulation.domain.SimulationResult;
import com.example.baseballorders.backend.simulation.infrastructure.persistence.PlayerDataRepository;
import com.example.baseballorders.messaging.SimulationPlayerMessage;
import com.example.baseballorders.messaging.SimulationRequestMessage;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** H2読込、SQS送信、相関結果待機を同期HTTP要求として調整する。 */
@Service
public final class SimulationCoordinator {

    private static final int LINEUP_SIZE = 9;
    private static final String MESSAGE_VERSION = "1";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final Logger LOGGER = LoggerFactory.getLogger(SimulationCoordinator.class);

    private final PlayerDataRepository playerDataRepository;
    private final SimulatorMessagePublisher publisher;
    private final WaitingResultRegistry registry;
    private final Duration timeout;

    /**
     * 仕様既定の30秒timeoutでCoordinatorを作成する。
     *
     * @param playerDataRepository player IDから選手データを取得するRepository
     * @param publisher SQS要求Publisher
     * @param registry HTTPとSQS結果の待機レジストリ
     */
    @Autowired
    public SimulationCoordinator(
            PlayerDataRepository playerDataRepository,
            SimulatorMessagePublisher publisher,
            WaitingResultRegistry registry) {
        this(playerDataRepository, publisher, registry, DEFAULT_TIMEOUT);
    }

    SimulationCoordinator(
            PlayerDataRepository playerDataRepository,
            SimulatorMessagePublisher publisher,
            WaitingResultRegistry registry,
            Duration timeout) {
        this.playerDataRepository = playerDataRepository;
        this.publisher = publisher;
        this.registry = registry;
        this.timeout = timeout;
    }

    /**
     * 選手データを読み込んでSQSへ要求し、相関する結果をtimeoutまで待機する。
     *
     * @param playerIds 打順どおりの9人のplayer ID
     * @return simulatorから受信した結果
     * @throws SimulationTimeoutException timeout内に結果を受信できなかった場合
     */
    public SimulationResult simulate(List<Long> playerIds) {
        if (playerIds.size() != LINEUP_SIZE) {
            throw new IllegalArgumentException("playerIds must contain exactly 9 entries");
        }
        var players = playerDataRepository.findAllByIds(playerIds);
        UUID simulationId = UUID.randomUUID();
        var waiting = registry.register(simulationId);
        LOGGER.info("simulation request accepted simulationId={}", simulationId);
        try {
            publisher.publish(
                    new SimulationRequestMessage(
                            simulationId,
                            MESSAGE_VERSION,
                            players.stream()
                                    .map(
                                            player ->
                                                    new SimulationPlayerMessage(
                                                            player.name(),
                                                            player.hitAverage(),
                                                            player.sluggish()))
                                    .toList()));
            SimulationResult result = waiting.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            LOGGER.info("simulation completed simulationId={}", simulationId);
            return result;
        } catch (TimeoutException exception) {
            LOGGER.warn("simulation timeout simulationId={}", simulationId);
            throw new SimulationTimeoutException(simulationId);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "simulation wait was interrupted: " + simulationId, exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("simulation result failed: " + simulationId, exception);
        } catch (RuntimeException exception) {
            LOGGER.error("simulation request send failed simulationId={}", simulationId, exception);
            throw new SimulationSendException(simulationId, exception);
        } finally {
            registry.remove(simulationId);
        }
    }
}

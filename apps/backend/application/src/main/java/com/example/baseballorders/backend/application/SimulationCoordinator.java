package com.example.baseballorders.backend.application;

import com.example.baseballorders.backend.application.adapter.PlayerDataRepository;
import com.example.baseballorders.backend.application.adapter.SimulatorMessagePublisher;
import com.example.baseballorders.backend.application.dto.SimulationRequest;
import com.example.baseballorders.backend.application.exception.SimulationSendException;
import com.example.baseballorders.backend.application.exception.SimulationTimeoutException;
import com.example.baseballorders.backend.simulation.domain.SimulationResult;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** シミュレーションユースケースのデータ取得、要求送信、結果待機を調整する。 */
public final class SimulationCoordinator {

    private static final int LINEUP_SIZE = 9;
    private static final String MESSAGE_VERSION = "1";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

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
    public SimulationCoordinator(
            PlayerDataRepository playerDataRepository,
            SimulatorMessagePublisher publisher,
            WaitingResultRegistry registry) {
        this(playerDataRepository, publisher, registry, DEFAULT_TIMEOUT);
    }

    /**
     * 指定したtimeoutでCoordinatorを作成する。
     *
     * @param playerDataRepository player IDから選手データを取得するRepository
     * @param publisher シミュレーション要求の送信ポート
     * @param registry HTTPと結果を相関するレジストリ
     * @param timeout 結果を待機する時間
     */
    public SimulationCoordinator(
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
        try {
            publisher.publish(new SimulationRequest(simulationId, MESSAGE_VERSION, players));
            SimulationResult result = waiting.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            return result;
        } catch (TimeoutException exception) {
            throw new SimulationTimeoutException(simulationId);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "simulation wait was interrupted: " + simulationId, exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("simulation result failed: " + simulationId, exception);
        } catch (RuntimeException exception) {
            throw new SimulationSendException(simulationId, exception);
        } finally {
            registry.remove(simulationId);
        }
    }
}

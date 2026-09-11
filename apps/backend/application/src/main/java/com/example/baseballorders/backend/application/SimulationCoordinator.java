package com.example.baseballorders.backend.application;

import com.example.baseballorders.backend.application.adapter.PlayerDataRepository;
import com.example.baseballorders.backend.application.adapter.SimulatorMessagePublisher;
import com.example.baseballorders.backend.application.dto.SimulationPlayerSelection;
import com.example.baseballorders.backend.application.dto.SimulationRequest;
import com.example.baseballorders.backend.application.exception.SimulationAcceptException;
import com.example.baseballorders.backend.application.exception.SimulationSendException;
import com.example.baseballorders.backend.application.exception.SimulationTimeoutException;
import com.example.baseballorders.backend.domain.PlayerData;
import com.example.baseballorders.backend.domain.Simulation;
import com.example.baseballorders.backend.domain.SimulationResult;
import com.example.baseballorders.backend.domain.SimulationStatus;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

/** シミュレーションユースケースのデータ取得、要求送信、結果待機を調整する。 */
public final class SimulationCoordinator {

    private static final int LINEUP_SIZE = 9;
    private static final String MESSAGE_VERSION = "1";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private final PlayerDataRepository playerDataRepository;
    private final SimulatorMessagePublisher publisher;
    private final WaitingResultRegistry registry;
    private final SimulationRepository simulationRepository;
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
        this(playerDataRepository, publisher, registry, null, DEFAULT_TIMEOUT);
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
        this(playerDataRepository, publisher, registry, null, timeout);
    }

    /**
     * 永続化を伴うCoordinatorを作成する。
     *
     * @param playerDataRepository player IDから選手データを取得するRepository
     * @param publisher シミュレーション要求の送信ポート
     * @param registry HTTPと結果を相関するレジストリ
     * @param simulationRepository シミュレーション状態の永続化ポート
     */
    public SimulationCoordinator(
            PlayerDataRepository playerDataRepository,
            SimulatorMessagePublisher publisher,
            WaitingResultRegistry registry,
            SimulationRepository simulationRepository) {
        this(playerDataRepository, publisher, registry, simulationRepository, DEFAULT_TIMEOUT);
    }

    private SimulationCoordinator(
            PlayerDataRepository playerDataRepository,
            SimulatorMessagePublisher publisher,
            WaitingResultRegistry registry,
            SimulationRepository simulationRepository,
            Duration timeout) {
        this.playerDataRepository = playerDataRepository;
        this.publisher = publisher;
        this.registry = registry;
        this.simulationRepository = simulationRepository;
        this.timeout = timeout;
    }

    /**
     * 選手データを読み込んでSQSへ要求し、相関する結果をtimeoutまで待機する。
     *
     * @param selections 打順どおりの9人のplayer IDとバント選択
     * @return simulatorから受信した結果
     * @throws SimulationTimeoutException timeout内に結果を受信できなかった場合
     */
    public SimulationResult simulate(List<SimulationPlayerSelection> selections) {
        return simulate(selections, null);
    }

    /**
     * 選手データを読み込んで所有者付き要求をSQSへ送信し、相関する結果を待機する。
     *
     * @param selections 打順どおりの9人のplayer IDとバント選択
     * @param userId ログイン済みユーザーID。未認証時はnull
     * @return simulatorから受信した結果
     * @throws SimulationTimeoutException timeout内に結果を受信できなかった場合
     */
    public SimulationResult simulate(List<SimulationPlayerSelection> selections, Long userId) {
        if (selections.size() != LINEUP_SIZE) {
            throw new IllegalArgumentException("playerIds must contain exactly 9 entries");
        }
        var playerData =
                playerDataRepository.findAllByIds(
                        selections.stream().map(SimulationPlayerSelection::playerId).toList());
        var players =
                IntStream.range(0, playerData.size())
                        .mapToObj(
                                index -> {
                                    var player = playerData.get(index);
                                    return new PlayerData(
                                            player.name(),
                                            player.hitAverage(),
                                            player.sluggish(),
                                            player.buntSuccessRate(),
                                            selections.get(index).buntEnabled(),
                                            player.stealSuccessRate());
                                })
                        .toList();
        UUID simulationId = UUID.randomUUID();
        if (simulationRepository != null) {
            simulationRepository.save(
                    new Simulation(
                            simulationId,
                            userId,
                            SimulationStatus.PENDING,
                            Clock.systemUTC().instant(),
                            null));
        }

        // 送信
        // simulation-idの登録
        var waiting = registry.register(simulationId);
        try {
            // SQSの送信
            publisher.publish(new SimulationRequest(simulationId, MESSAGE_VERSION, players));
        } catch (RuntimeException exception) {
            registry.remove(simulationId);
            throw new SimulationSendException(simulationId, exception);
        }

        // 受信
        SimulationResult result;
        try {
            // simulation-idの取得
            result = waiting.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            throw new SimulationTimeoutException(simulationId);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "simulation wait was interrupted: " + simulationId, exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("simulation result failed: " + simulationId, exception);
        } catch (RuntimeException exception) {
            throw new SimulationAcceptException(simulationId, exception);
        } finally {
            registry.remove(simulationId);
        }

        return result;
    }
}

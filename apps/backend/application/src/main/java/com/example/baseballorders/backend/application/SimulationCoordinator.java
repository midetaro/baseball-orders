package com.example.baseballorders.backend.application;

import com.example.baseballorders.backend.application.adapter.SimulatorMessagePublisher;
import com.example.baseballorders.backend.application.dto.SimulationRequestBuilder;
import com.example.baseballorders.backend.application.exception.SimulationAcceptException;
import com.example.baseballorders.backend.application.exception.SimulationSendException;
import com.example.baseballorders.backend.application.exception.SimulationTimeoutException;
import com.example.baseballorders.backend.domain.PlayerData;
import com.example.baseballorders.backend.domain.SimulationMode;
import com.example.baseballorders.backend.domain.SimulationResult;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** シミュレーションユースケースのデータ取得、要求送信、結果待機を調整する。 */
public final class SimulationCoordinator {

    /** 打順の人数は野球のルールで固定であり設定値ではない。 */
    private static final int LINEUP_SIZE = 9;

    /** 共有メッセージのスキーマ版数であり設定値ではない。 */
    private static final String MESSAGE_VERSION = "1";

    /** 確率の定義そのものを表す下限であり設定値ではない。 */
    private static final float MINIMUM_SUCCESS_RATE = 0.000f;

    private final SimulatorMessagePublisher publisher;
    private final WaitingResultRegistry registry;
    private final SimulationLimits limits;

    /**
     * 設定から注入された上限値でCoordinatorを作成する。
     *
     * @param publisher シミュレーション要求の送信ポート
     * @param registry HTTPと結果を相関するレジストリ
     * @param limits 結果待機時間と打順受付の上限値
     */
    public SimulationCoordinator(
            SimulatorMessagePublisher publisher,
            WaitingResultRegistry registry,
            SimulationLimits limits) {
        this.publisher = publisher;
        this.registry = registry;
        this.limits = limits;
    }

    /**
     * 画面入力された選手データをSQSへ要求し、相関する結果をtimeoutまで待機する。
     *
     * @param players 打順どおりの9人の入力済み選手データ
     * @return simulatorから受信した結果
     * @throws SimulationTimeoutException timeout内に結果を受信できなかった場合
     */
    public SimulationResult simulate(List<PlayerData> players) {
        return simulate(players, SimulationMode.LARGE_SCALE_RUN);
    }

    /**
     * 画面入力された選手データを指定した試合実行モードでSQSへ要求し、相関する結果をtimeoutまで待機する。
     *
     * @param players 打順どおりの9人の入力済み選手データ
     * @param mode 大規模実行と1試合実行を見分ける試合実行モード
     * @return simulatorから受信した結果
     * @throws SimulationTimeoutException timeout内に結果を受信できなかった場合
     */
    public SimulationResult simulate(List<PlayerData> players, SimulationMode mode) {
        if (players.size() != LINEUP_SIZE) {
            throw new IllegalArgumentException("players must contain exactly 9 entries");
        }
        validateLineup(players);
        UUID simulationId = UUID.randomUUID();

        // 送信
        // simulation-idの登録
        var waiting = registry.register(simulationId);
        try {
            // SQSの送信
            publisher.publish(
                    SimulationRequestBuilder.simulationRequest()
                            .simulationId(simulationId)
                            .version(MESSAGE_VERSION)
                            .players(players)
                            .mode(mode)
                            .build());
        } catch (RuntimeException exception) {
            registry.remove(simulationId);
            throw new SimulationSendException(simulationId, exception);
        }

        // 受信
        SimulationResult result;
        try {
            // simulation-idの取得
            result = waiting.get(limits.resultTimeout().toMillis(), TimeUnit.MILLISECONDS);
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

    private void validateLineup(List<PlayerData> players) {
        double totalHitAverage = 0;
        double totalSluggish = 0;
        for (PlayerData player : players) {
            totalHitAverage += player.hitAverage();
            totalSluggish += player.sluggish();
            requireSuccessRate(player.buntSuccessRate(), "buntSuccessRate");
            requireSuccessRate(player.stealSuccessRate(), "stealSuccessRate");
        }
        if (totalHitAverage / LINEUP_SIZE > limits.maximumAverageHitAverage()) {
            throw new IllegalArgumentException(
                    "the average hitAverage must not exceed " + limits.maximumAverageHitAverage());
        }
        if (totalSluggish / LINEUP_SIZE > limits.maximumAverageSluggish()) {
            throw new IllegalArgumentException(
                    "the average sluggish must not exceed " + limits.maximumAverageSluggish());
        }
    }

    private void requireSuccessRate(float value, String name) {
        if (value > limits.maximumSuccessRate()) {
            throw new IllegalArgumentException(
                    name
                            + " must be between "
                            + MINIMUM_SUCCESS_RATE
                            + " and "
                            + limits.maximumSuccessRate());
        }
    }
}

package com.example.baseballorders.backend.application;

import com.example.baseballorders.backend.application.adapter.SimulatorClient;
import com.example.baseballorders.backend.application.dto.SimulationRequestBuilder;
import com.example.baseballorders.backend.application.exception.SimulationSendException;
import com.example.baseballorders.backend.application.exception.SimulationTimeoutException;
import com.example.baseballorders.backend.domain.PlayerData;
import com.example.baseballorders.backend.domain.SimulationMode;
import com.example.baseballorders.backend.domain.SimulationResult;
import java.util.List;
import java.util.UUID;

/** シミュレーションユースケースの入力検証、要求の組み立て、simulatorへの同期呼び出しを調整する。 */
public final class SimulationCoordinator {

    /** 打順の人数は野球のルールで固定であり設定値ではない。 */
    private static final int LINEUP_SIZE = 9;

    private final SimulatorClient simulatorClient;
    private final SimulationLimits limits;

    /**
     * 設定から注入された上限値でCoordinatorを作成する。
     *
     * @param simulatorClient simulatorへ要求を送り結果を同期的に受け取るポート
     * @param limits 打順受付の上限値
     */
    public SimulationCoordinator(SimulatorClient simulatorClient, SimulationLimits limits) {
        this.simulatorClient = simulatorClient;
        this.limits = limits;
    }

    /**
     * 画面入力された選手データを大規模実行モードでsimulatorへ要求し、その応答を返す。
     *
     * @param players 打順どおりの9人の入力済み選手データ
     * @return simulatorから受け取った結果
     * @throws IllegalArgumentException 選手が9人でない場合、または打率平均が上限を超える場合
     * @throws SimulationTimeoutException 許容時間内にsimulatorが応答しなかった場合
     * @throws SimulationSendException simulatorへ到達できない場合、またはsimulatorがエラーを返した場合
     */
    public SimulationResult simulate(List<PlayerData> players) {
        return simulate(players, SimulationMode.LARGE_SCALE_RUN);
    }

    /**
     * 画面入力された選手データを指定した試合実行モードでsimulatorへ要求し、その応答を返す。
     *
     * @param players 打順どおりの9人の入力済み選手データ
     * @param mode 大規模実行と1試合実行を見分ける試合実行モード
     * @return simulatorから受け取った結果
     * @throws IllegalArgumentException 選手が9人でない場合、または打率平均が上限を超える場合
     * @throws SimulationTimeoutException 許容時間内にsimulatorが応答しなかった場合
     * @throws SimulationSendException simulatorへ到達できない場合、またはsimulatorがエラーを返した場合
     */
    public SimulationResult simulate(List<PlayerData> players, SimulationMode mode) {
        if (players.size() != LINEUP_SIZE) {
            throw new IllegalArgumentException("players must contain exactly 9 entries");
        }
        validateLineup(players);
        return simulatorClient.simulate(
                SimulationRequestBuilder.simulationRequest()
                        .simulationId(UUID.randomUUID())
                        .players(players)
                        .mode(mode)
                        .build());
    }

    private void validateLineup(List<PlayerData> players) {
        double totalHitAverage = 0;
        for (PlayerData player : players) {
            totalHitAverage += player.hitAverage();
        }
        if (totalHitAverage / LINEUP_SIZE > limits.maximumAverageHitAverage()) {
            throw new IllegalArgumentException(
                    "the average hitAverage must not exceed " + limits.maximumAverageHitAverage());
        }
    }
}

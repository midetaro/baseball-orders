package com.example.baseballorders.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * simulatorからbackendへ返す共有結果メッセージ。
 *
 * @param simulationId 要求と同じ相関ID
 * @param version メッセージスキーマのバージョン
 * @param statistics 全試合の得点統計
 */
public record SimulationResultMessage(
        @JsonProperty("simulation_id") UUID simulationId, String version, Statistics statistics) {

    /**
     * 全試合の得点統計。
     *
     * @param averageScore 平均得点
     * @param medianScore 中央値得点
     * @param maximumScore 最大得点
     * @param gameCount シミュレーションした試合数
     * @param scoreDistribution 得点ごとの試合数
     * @param homeRunCount 本塁打数
     * @param soloHomeRunCount ソロ本塁打数
     * @param twoRunHomeRunCount ツーラン本塁打数
     * @param threeRunHomeRunCount スリーラン本塁打数
     * @param grandSlamCount 満塁本塁打数
     * @param buntCount 成功バント数
     * @param stealCount 成功盗塁数
     */
    public record Statistics(
            double averageScore,
            double medianScore,
            int maximumScore,
            int gameCount,
            Map<Integer, Integer> scoreDistribution,
            int homeRunCount,
            int soloHomeRunCount,
            int twoRunHomeRunCount,
            int threeRunHomeRunCount,
            int grandSlamCount,
            int buntCount,
            int stealCount) {

        /**
         * Creates score-only statistics with no batting-event counts.
         *
         * @param averageScore 平均得点
         * @param medianScore 中央値得点
         * @param maximumScore 最大得点
         */
        public Statistics(double averageScore, double medianScore, int maximumScore) {
            this(averageScore, medianScore, maximumScore, 0, Map.of(), 0, 0, 0, 0, 0, 0, 0);
        }
    }

    /**
     * Creates a result message while discarding legacy per-game results.
     *
     * @param simulationId 要求と同じ相関ID
     * @param version メッセージスキーマのバージョン
     * @param ignoredResults 廃止された試合ごとの結果
     * @param statistics 画面表示用の集計統計
     */
    public SimulationResultMessage(
            UUID simulationId, String version, List<Result> ignoredResults, Statistics statistics) {
        this(simulationId, version, statistics);
    }

    /**
     * Creates a result message with no statistics while discarding legacy per-game results.
     *
     * @param simulationId 要求と同じ相関ID
     * @param version メッセージスキーマのバージョン
     * @param ignoredResults 廃止された試合ごとの結果
     */
    public SimulationResultMessage(UUID simulationId, String version, List<Result> ignoredResults) {
        this(simulationId, version, (Statistics) null);
    }

    /** 廃止された1試合の結果を表す移行用型。 */
    public record Result(int score, int runs) {}
}

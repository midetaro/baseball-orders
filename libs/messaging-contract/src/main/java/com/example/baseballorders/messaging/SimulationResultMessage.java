package com.example.baseballorders.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;

/**
 * simulatorからbackendへ返す共有結果メッセージ。
 *
 * @param simulationId 要求と同じ相関ID
 * @param version メッセージスキーマのバージョン
 * @param results 実行順の得点・失点の組
 * @param statistics 全試合の得点統計
 */
public record SimulationResultMessage(
        @JsonProperty("simulation_id") UUID simulationId,
        String version,
        List<Result> results,
        Statistics statistics) {

    /**
     * 全試合の得点統計。
     *
     * @param averageScore 平均得点
     * @param medianScore 中央値得点
     * @param maximumScore 最大得点
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
            this(averageScore, medianScore, maximumScore, 0, 0, 0, 0, 0, 0, 0);
        }
    }

    /**
     * Backwards-compatible constructor for callers that do not yet supply statistics.
     *
     * @param simulationId 要求と同じ相関ID
     * @param version メッセージスキーマのバージョン
     * @param results 実行順の得点・失点の組
     */
    public SimulationResultMessage(UUID simulationId, String version, List<Result> results) {
        this(simulationId, version, results, null);
    }

    /**
     * 1試合の結果。
     *
     * @param score 得点
     * @param runs 失点
     */
    public record Result(int score, int runs) {}
}

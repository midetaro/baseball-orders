package com.example.baseballorders.backend.domain;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jilt.Builder;
import org.jilt.BuilderStyle;

/**
 * backendがHTTP要求へ返すシミュレーション結果。
 *
 * @param simulationId シミュレーションの相関ID
 * @param results 実行順の得点・失点の組
 * @param statistics 全試合の得点統計
 */
@Builder(style = BuilderStyle.STAGED)
public record SimulationResult(UUID simulationId, List<Result> results, Statistics statistics) {
    /** 統計情報を必須にしてシミュレーション結果を作成する。 */
    public SimulationResult {
        Objects.requireNonNull(simulationId, "simulationId must not be null");
        Objects.requireNonNull(results, "results must not be null");
        Objects.requireNonNull(statistics, "statistics must not be null");
    }

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
    @Builder(style = BuilderStyle.STAGED)
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
     * 1試合の結果。
     *
     * @param score 得点
     * @param runs 失点
     */
    @Builder(style = BuilderStyle.STAGED)
    public record Result(int score, int runs) {}
}

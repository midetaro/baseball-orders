package com.example.baseballorders.backend.domain;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.jilt.Builder;
import org.jilt.BuilderStyle;

/**
 * backendがHTTP要求へ返すシミュレーション結果。
 *
 * @param simulationId シミュレーションの相関ID
 * @param statistics 全試合の得点統計
 */
@Builder(style = BuilderStyle.STAGED)
public record SimulationResult(UUID simulationId, Statistics statistics) {
    /** 統計情報を必須にしてシミュレーション結果を作成する。 */
    public SimulationResult {
        Objects.requireNonNull(simulationId, "simulationId must not be null");
        Objects.requireNonNull(statistics, "statistics must not be null");
    }

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
     * @param buntFailureCount 失敗バント数
     * @param stealFailureCount 失敗盗塁数
     * @param advancingBuntCount 進塁バント成功数
     * @param squeezeBuntCount スクイズ成功数
     * @param advancingBuntFailureCount 進塁バント失敗数
     * @param squeezeBuntFailureCount スクイズ失敗数
     * @param stealToSecondCount 二盗成功数
     * @param stealToThirdCount 三盗成功数
     */
    @Builder(style = BuilderStyle.STAGED)
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
            int stealCount,
            int buntFailureCount,
            int stealFailureCount,
            int advancingBuntCount,
            int squeezeBuntCount,
            int advancingBuntFailureCount,
            int squeezeBuntFailureCount,
            int stealToSecondCount,
            int stealToThirdCount) {

        /**
         * Creates statistics with no failed tactical-play counts.
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
        public Statistics(
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
            this(
                    averageScore,
                    medianScore,
                    maximumScore,
                    gameCount,
                    scoreDistribution,
                    homeRunCount,
                    soloHomeRunCount,
                    twoRunHomeRunCount,
                    threeRunHomeRunCount,
                    grandSlamCount,
                    buntCount,
                    stealCount,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0);
        }

        /**
         * Creates score-only statistics with no batting-event counts.
         *
         * @param averageScore 平均得点
         * @param medianScore 中央値得点
         * @param maximumScore 最大得点
         */
        public Statistics(double averageScore, double medianScore, int maximumScore) {
            this(
                    averageScore,
                    medianScore,
                    maximumScore,
                    0,
                    Map.of(),
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0);
        }
    }

    /**
     * Creates a result while discarding legacy per-game results.
     *
     * @param simulationId シミュレーションの相関ID
     * @param ignoredResults 廃止された試合ごとの結果
     * @param statistics 画面表示用の集計統計
     */
    public SimulationResult(UUID simulationId, List<Result> ignoredResults, Statistics statistics) {
        this(simulationId, statistics);
    }

    /** 廃止された1試合の結果を表す移行用型。 */
    public record Result(int score, int runs) {}
}

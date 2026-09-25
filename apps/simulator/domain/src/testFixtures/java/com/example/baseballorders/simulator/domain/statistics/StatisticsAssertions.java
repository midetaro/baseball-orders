package com.example.baseballorders.simulator.domain.statistics;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

/**
 * 統計の恒等式をまとめたアサーション。
 *
 * <p>統計を触るテストは期待値の丸ごと比較に加えてこれを併用する。内訳の合計が総数と一致しない 記録漏れは、個別フィールドの期待値をそろえても見逃されるため、恒等式として独立に検証する。
 */
public final class StatisticsAssertions {

    private StatisticsAssertions() {}

    /**
     * 試合統計の内訳と総数が整合していることを検証する。
     *
     * @param statistics 検証する試合統計
     */
    public static void assertConsistent(GameStatistics statistics) {
        assertAll(
                "GameStatisticsの恒等式",
                () ->
                        assertEquals(
                                statistics.singleHitCount()
                                        + statistics.doubleHitCount()
                                        + statistics.tripleHitCount()
                                        + statistics.homeRunCount(),
                                statistics.hitCount(),
                                "総安打数は単打・二塁打・三塁打・本塁打の合計であること"),
                () ->
                        assertEquals(
                                statistics.soloHomeRunCount()
                                        + statistics.twoRunHomeRunCount()
                                        + statistics.threeRunHomeRunCount()
                                        + statistics.grandSlamCount(),
                                statistics.homeRunCount(),
                                "本塁打数は打点別内訳の合計であること"),
                () ->
                        assertEquals(
                                statistics.advancingBuntCount() + statistics.squeezeBuntCount(),
                                statistics.buntCount(),
                                "成功バント数は進塁バントとスクイズの合計であること"),
                () ->
                        assertEquals(
                                statistics.advancingBuntFailureCount()
                                        + statistics.squeezeBuntFailureCount(),
                                statistics.buntFailureCount(),
                                "失敗バント数は進塁バントとスクイズの合計であること"),
                () ->
                        assertEquals(
                                statistics.stealToSecondCount() + statistics.stealToThirdCount(),
                                statistics.stealCount(),
                                "成功盗塁数は二盗と三盗の合計であること"));
    }

    /**
     * 集計統計の内訳・総数・得点分布が整合していることを検証する。
     *
     * @param statistics 検証する集計統計
     */
    public static void assertConsistent(ScoreStatistics statistics) {
        // 度数合計・最大得点・加重合計を1回の走査で集計する。
        Map<Integer, Integer> distribution = statistics.scoreDistribution();
        int distributedGameCount = 0;
        int distributedMaximumScore = Integer.MIN_VALUE;
        double weightedTotal = 0.0;
        for (Map.Entry<Integer, Integer> entry : distribution.entrySet()) {
            int score = entry.getKey();
            int games = entry.getValue();
            distributedGameCount += games;
            distributedMaximumScore = Math.max(distributedMaximumScore, score);
            weightedTotal += (double) score * games;
        }
        int observedGameCount = distributedGameCount;
        int observedMaximumScore = distributedMaximumScore;
        double distributedAverageScore = weightedTotal / distributedGameCount;
        assertAll(
                "ScoreStatisticsの恒等式",
                () ->
                        assertEquals(
                                statistics.singleHitCount()
                                        + statistics.doubleHitCount()
                                        + statistics.tripleHitCount()
                                        + statistics.homeRunCount(),
                                statistics.hitCount(),
                                "総安打数は単打・二塁打・三塁打・本塁打の合計であること"),
                () ->
                        assertEquals(
                                statistics.soloHomeRunCount()
                                        + statistics.twoRunHomeRunCount()
                                        + statistics.threeRunHomeRunCount()
                                        + statistics.grandSlamCount(),
                                statistics.homeRunCount(),
                                "本塁打数は打点別内訳の合計であること"),
                () ->
                        assertEquals(
                                statistics.advancingBuntCount() + statistics.squeezeBuntCount(),
                                statistics.buntCount(),
                                "成功バント数は進塁バントとスクイズの合計であること"),
                () ->
                        assertEquals(
                                statistics.advancingBuntFailureCount()
                                        + statistics.squeezeBuntFailureCount(),
                                statistics.buntFailureCount(),
                                "失敗バント数は進塁バントとスクイズの合計であること"),
                () ->
                        assertEquals(
                                statistics.stealToSecondCount() + statistics.stealToThirdCount(),
                                statistics.stealCount(),
                                "成功盗塁数は二盗と三盗の合計であること"),
                () ->
                        assertEquals(
                                statistics.gameCount(), observedGameCount, "得点分布の度数合計は試合数と一致すること"),
                () ->
                        assertEquals(
                                statistics.maximumScore(),
                                observedMaximumScore,
                                "最大得点は得点分布のキーの最大と一致すること"),
                () ->
                        assertEquals(
                                distributedAverageScore,
                                statistics.averageScore(),
                                1.0e-9,
                                "平均得点は得点分布と整合すること"),
                () ->
                        assertTrue(
                                statistics.medianScore() >= 0
                                        && statistics.medianScore() <= statistics.maximumScore(),
                                "中央値は0以上かつ最大得点以下であること"));
    }
}

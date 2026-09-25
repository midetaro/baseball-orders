package com.example.baseballorders.simulator.application;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.baseballorders.simulator.application.contract.SimulationResult;
import com.example.baseballorders.simulator.application.usecase.SimulateGameUseCase;
import com.example.baseballorders.simulator.domain.game.InningScript;
import com.example.baseballorders.simulator.domain.game.ScriptedBaseStateFactory;
import com.example.baseballorders.simulator.domain.player.BatterTestData;
import com.example.baseballorders.simulator.domain.player.strategy.Draws;
import com.example.baseballorders.simulator.domain.player.strategy.ScriptedRandom;
import com.example.baseballorders.simulator.domain.statistics.ScoreStatistics;
import com.example.baseballorders.simulator.domain.statistics.ScoreStatisticsBuilder;
import com.example.baseballorders.simulator.domain.statistics.StatisticsAssertions;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 集計統計の決定論シナリオ。
 *
 * <p>実物: SimulateGameUseCase, ScoreAccumulator, GameBattingContext, InningStateContext,
 * GameStatisticsRecorder, BatterEntity, 各 Strategy
 *
 * <p>モック: BaseStateFactory / BasesState（{@code ScriptedBaseStateFactory} で試合ごとのイニング進行を脚本化）,
 * RandomGenerator（乱数列を固定）
 *
 * <p>担保する疎通: ユースケース -> 試合の反復実行 -> GameBattingContext -> 試合終了通知 -> ScoreAccumulator ->
 * ScoreStatistics の平均・中央値・最大・試合数・得点分布・各カウンタの合算
 *
 * <p>担保しないもの: 走者配置ごとの進塁・得点規則（L2 の各 {@code BasesState} テストが担保する）、 SQS との疎通（infrastructure と
 * integration-test の担当）
 *
 * <p>既存の {@code SimulateGameUseCaseTest} は実乱数での恒等式だけを見るスモークテストとして残し、
 * 決定論的な期待値検証はこのクラスが担う。中央値は試合数の偶奇で計算が分岐するため 両方のケースを書く。
 */
class SimulateGameUseCaseScenarioTest {

    private static final int AT_BATS_PER_GAME = 27;

    private static ScoreStatistics expectedStatistics(
            double averageScore,
            double medianScore,
            int maximumScore,
            int gameCount,
            Map<Integer, Integer> scoreDistribution,
            int soloHomeRuns) {
        return ScoreStatisticsBuilder.scoreStatistics()
                .averageScore(averageScore)
                .medianScore(medianScore)
                .maximumScore(maximumScore)
                .gameCount(gameCount)
                .scoreDistribution(scoreDistribution)
                .hitCount(soloHomeRuns)
                .singleHitCount(0)
                .doubleHitCount(0)
                .tripleHitCount(0)
                .homeRunCount(soloHomeRuns)
                .soloHomeRunCount(soloHomeRuns)
                .twoRunHomeRunCount(0)
                .threeRunHomeRunCount(0)
                .grandSlamCount(0)
                .buntCount(0)
                .stealCount(0)
                .buntFailureCount(0)
                .stealFailureCount(0)
                .advancingBuntCount(0)
                .squeezeBuntCount(0)
                .advancingBuntFailureCount(0)
                .squeezeBuntFailureCount(0)
                .stealToSecondCount(0)
                .stealToThirdCount(0)
                .build();
    }

    /** 各試合の先頭打席だけを本塁打にし、残りを三振にした乱数列を作る。 */
    private static float[] script(int gameCount) {
        float[] wholeRun = new float[0];
        for (int game = 0; game < gameCount; game++) {
            wholeRun =
                    ScriptedRandom.concat(
                            wholeRun,
                            new float[] {Draws.HOMER},
                            ScriptedRandom.repeat(Draws.STRIKEOUT, AT_BATS_PER_GAME - 1));
        }
        return wholeRun;
    }

    @Test
    @DisplayName("奇数試合では中央値に中央の得点を採り、得点分布と各カウンタを全試合から合算する")
    void aggregatesOddNumberOfGames() {
        // given
        var factory =
                ScriptedBaseStateFactory.ofGames(
                        InningScript.ofRunsPerInning(1, 0, 0, 0, 0, 0, 0, 0, 0),
                        InningScript.ofRunsPerInning(1, 1, 1, 1, 1, 0, 0, 0, 0),
                        InningScript.ofRunsPerInning(2, 1, 0, 0, 0, 0, 0, 0, 0));
        var sut = new SimulateGameUseCase(3, factory);

        // when
        SimulationResult result;
        try (ScriptedRandom random = ScriptedRandom.of(script(3))) {
            result = sut.invoke(BatterTestData.swingOnlyLineUp());

            // then
            ScoreStatistics statistics = result.statistics();
            assertAll(
                    () ->
                            assertEquals(
                                    expectedStatistics(3.0, 3.0, 5, 3, Map.of(1, 1, 3, 1, 5, 1), 3),
                                    statistics),
                    () ->
                            assertEquals(
                                    List.of(1, 3, 5),
                                    List.copyOf(statistics.scoreDistribution().keySet()),
                                    "得点分布は得点の昇順であること"),
                    () -> StatisticsAssertions.assertConsistent(statistics),
                    () ->
                            assertEquals(
                                    3 * AT_BATS_PER_GAME,
                                    random.consumedCount(),
                                    "1試合27打席ぶんの乱数を消費すること"),
                    () -> random.assertFullyConsumed(),
                    () -> factory.assertFullyPlayed(),
                    () -> assertEquals(3, factory.startedGameCount()));
        }
    }

    @Test
    @DisplayName("偶数試合では中央値に中央2試合の平均を採り、得点分布と各カウンタを全試合から合算する")
    void aggregatesEvenNumberOfGames() {
        // given
        var factory =
                ScriptedBaseStateFactory.ofGames(
                        InningScript.ofRunsPerInning(0, 0, 0, 0, 0, 0, 0, 0, 0),
                        InningScript.ofRunsPerInning(2, 0, 0, 0, 0, 0, 0, 0, 0),
                        InningScript.ofRunsPerInning(4, 0, 0, 0, 0, 0, 0, 0, 0),
                        InningScript.ofRunsPerInning(10, 0, 0, 0, 0, 0, 0, 0, 0));
        var sut = new SimulateGameUseCase(4, factory);

        // when
        SimulationResult result;
        try (ScriptedRandom random = ScriptedRandom.of(script(4))) {
            result = sut.invoke(BatterTestData.swingOnlyLineUp());

            // then
            ScoreStatistics statistics = result.statistics();
            assertAll(
                    () ->
                            assertEquals(
                                    expectedStatistics(
                                            4.0, 3.0, 10, 4, Map.of(0, 1, 2, 1, 4, 1, 10, 1), 4),
                                    statistics),
                    () ->
                            assertEquals(
                                    List.of(0, 2, 4, 10),
                                    List.copyOf(statistics.scoreDistribution().keySet()),
                                    "得点分布は得点の昇順であること"),
                    () -> StatisticsAssertions.assertConsistent(statistics),
                    () ->
                            assertEquals(
                                    4 * AT_BATS_PER_GAME,
                                    random.consumedCount(),
                                    "1試合27打席ぶんの乱数を消費すること"),
                    () -> random.assertFullyConsumed(),
                    () -> factory.assertFullyPlayed(),
                    () -> assertEquals(4, factory.startedGameCount()));
        }
    }
}

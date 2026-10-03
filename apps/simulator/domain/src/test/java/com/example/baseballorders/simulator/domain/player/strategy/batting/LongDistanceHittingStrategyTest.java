package com.example.baseballorders.simulator.domain.player.strategy.batting;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.example.baseballorders.simulator.domain.play.BattingResult;
import com.example.baseballorders.simulator.domain.player.strategy.ScriptedRandom;
import com.example.baseballorders.simulator.domain.rule.HittingDistribution;
import com.example.baseballorders.simulator.domain.rule.HittingDistributionBuilder;
import com.example.baseballorders.simulator.domain.rule.SimulationRulesTestData;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** 四球を除いた打席に対する打率と、設定された長打配分の確率仕様。 */
class LongDistanceHittingStrategyTest {

    private static final float BATTING_AVERAGE = 0.400f;
    private static final float SLUGGING = 0.550f;

    static Stream<Arguments> battingBoundaries() {
        return Stream.of(
                arguments("乱数0は四球", 0.0f, BattingResult.WALK),
                arguments("0.05の直前", Math.nextDown(0.05f), BattingResult.WALK),
                arguments("0.05と等しい境界", 0.05f, BattingResult.HIT_SINGLE),
                arguments("0.31206897の直前", Math.nextDown(0.31206897f), BattingResult.HIT_SINGLE),
                arguments("0.31206897と等しい境界", 0.31206897f, BattingResult.HIT_DOUBLE),
                arguments("0.33172414の直前", Math.nextDown(0.33172414f), BattingResult.HIT_DOUBLE),
                arguments("0.33172414と等しい境界", 0.33172414f, BattingResult.HIT_TRIPLE),
                arguments("0.3513793の直前", Math.nextDown(0.3513793f), BattingResult.HIT_TRIPLE),
                arguments("0.3513793と等しい境界", 0.3513793f, BattingResult.HIT_HOMER),
                arguments("0.43の直前", Math.nextDown(0.43f), BattingResult.HIT_HOMER),
                arguments("0.43と等しい境界", 0.43f, BattingResult.STRIKEOUT),
                arguments("0.5725の直前", Math.nextDown(0.5725f), BattingResult.STRIKEOUT),
                arguments("0.5725と等しい境界", 0.5725f, BattingResult.BATTED_OUT),
                arguments("乱数1は凡退", 1.0f, BattingResult.BATTED_OUT));
    }

    @DisplayName("長距離打者は乱数区間の境界どおりに打席結果を決定し、乱数を1個だけ消費する")
    @ParameterizedTest(name = "{0} -> {2}")
    @MethodSource("battingBoundaries")
    void determinesBattingResultAtBoundary(
            String description, float random, BattingResult expectedResult) {
        // given
        var sut =
                new LongDistanceHittingStrategy(
                        SimulationRulesTestData.standard().batting(),
                        SimulationRulesTestData.standard().longDistanceHitting());

        // when
        BattingResult result;
        try (ScriptedRandom scriptedRandom = ScriptedRandom.of(random)) {
            result = sut.batting(BATTING_AVERAGE, SLUGGING);

            // then
            assertAll(
                    description,
                    () -> assertEquals(expectedResult, result),
                    () -> assertEquals(1, scriptedRandom.consumedCount()),
                    () -> scriptedRandom.assertFullyConsumed());
        }
    }

    static Stream<Arguments> distanceComparisons() {
        return Stream.of(
                arguments(
                        "0.32は長距離で二塁打・中距離で単打",
                        0.32f,
                        BattingResult.HIT_DOUBLE,
                        BattingResult.HIT_SINGLE),
                arguments(
                        "0.365は長距離で本塁打・中距離で二塁打",
                        0.365f,
                        BattingResult.HIT_HOMER,
                        BattingResult.HIT_DOUBLE),
                arguments(
                        "0.395は長距離で本塁打・中距離で三塁打",
                        0.395f,
                        BattingResult.HIT_HOMER,
                        BattingResult.HIT_TRIPLE));
    }

    @DisplayName("同じ成績と乱数でも長距離打者は中距離打者より長打側の結果になる")
    @ParameterizedTest(name = "{0}")
    @MethodSource("distanceComparisons")
    void producesLongerHitThanMiddleDistance(
            String description,
            float random,
            BattingResult expectedLongResult,
            BattingResult expectedMiddleResult) {
        // given
        var sut =
                new LongDistanceHittingStrategy(
                        SimulationRulesTestData.standard().batting(),
                        SimulationRulesTestData.standard().longDistanceHitting());
        var middleDistance =
                new MiddleDistanceHittingStrategy(
                        SimulationRulesTestData.standard().batting(),
                        SimulationRulesTestData.standard().middleDistanceHitting());

        // when
        BattingResult longResult;
        try (ScriptedRandom scriptedRandom = ScriptedRandom.of(random)) {
            longResult = sut.batting(BATTING_AVERAGE, SLUGGING);
            scriptedRandom.assertFullyConsumed();
        }
        BattingResult middleResult;
        try (ScriptedRandom scriptedRandom = ScriptedRandom.of(random)) {
            middleResult = middleDistance.batting(BATTING_AVERAGE, SLUGGING);
            scriptedRandom.assertFullyConsumed();
        }

        // then
        assertAll(
                description,
                () -> assertEquals(expectedLongResult, longResult),
                () -> assertEquals(expectedMiddleResult, middleResult));
    }

    static Stream<Arguments> configuredDistributions() {
        return Stream.of(
                arguments(
                        "既定の配分では0.3800000は本塁打",
                        SimulationRulesTestData.standard().longDistanceHitting(),
                        BattingResult.HIT_HOMER),
                arguments(
                        "本塁打除数を8にすると0.3800000は二塁打",
                        HittingDistributionBuilder.hittingDistribution()
                                .doubleDivisor(8)
                                .tripleDivisor(8)
                                .homeRunDivisor(8)
                                .singleReductionDivisor(1)
                                .build(),
                        BattingResult.HIT_DOUBLE));
    }

    @DisplayName("長距離打者の長打配分は設定された除数で決まる")
    @ParameterizedTest(name = "{0}")
    @MethodSource("configuredDistributions")
    void usesConfiguredHittingDistribution(
            String description, HittingDistribution distribution, BattingResult expectedResult) {
        // given
        var sut =
                new LongDistanceHittingStrategy(
                        SimulationRulesTestData.standard().batting(), distribution);

        // when
        BattingResult result;
        try (ScriptedRandom scriptedRandom = ScriptedRandom.of(0.38f)) {
            result = sut.batting(BATTING_AVERAGE, SLUGGING);

            // then
            assertAll(
                    description,
                    () -> assertEquals(expectedResult, result),
                    scriptedRandom::assertFullyConsumed);
        }
    }
}

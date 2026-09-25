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

/**
 * 長距離打者の確率仕様。
 *
 * <p>出塁率 0.400 / 長打率 0.550 の基準打者での乱数区間は次のとおり。長打分 0.150 を本塁打へ 1/2、二塁打・三塁打へ 1/8 ずつ配分し、単打を中距離打者より削る。
 *
 * <pre>
 * [0.0000000, 0.0500000) 四球
 * [0.0500000, 0.2913793) 単打
 * [0.2913793, 0.3094828) 二塁打
 * [0.3094828, 0.3275862) 三塁打
 * [0.3275862, 0.4000000) 本塁打
 * [0.4000000, 0.5500000) 三振
 * [0.5500000, 1.0000000] 凡退
 * </pre>
 *
 * <p>中距離打者との違いは本塁打区間が広く単打区間が狭いことにある。境界値でその差を固定する。
 */
class LongDistanceHittingStrategyTest {

    private static final float ON_BASE_PERCENTAGE = 0.400f;
    private static final float SLUGGING = 0.550f;

    static Stream<Arguments> battingBoundaries() {
        return Stream.of(
                arguments("0.0000000は四球区間の下端", 0.0f, BattingResult.WALK),
                arguments("0.049999997は四球区間の直前", 0.049999997f, BattingResult.WALK),
                arguments("0.0500000は単打区間の下端", 0.05f, BattingResult.HIT_SINGLE),
                arguments("0.29137927は単打区間の直前", 0.29137927f, BattingResult.HIT_SINGLE),
                arguments("0.2913793は二塁打区間の下端", 0.2913793f, BattingResult.HIT_DOUBLE),
                arguments("0.30948272は二塁打区間の直前", 0.30948272f, BattingResult.HIT_DOUBLE),
                arguments("0.30948275は三塁打区間の下端", 0.30948275f, BattingResult.HIT_TRIPLE),
                arguments("0.32758617は三塁打区間の直前", 0.32758617f, BattingResult.HIT_TRIPLE),
                arguments("0.3275862は本塁打区間の下端", 0.3275862f, BattingResult.HIT_HOMER),
                arguments("0.39999998は本塁打区間の直前", 0.39999998f, BattingResult.HIT_HOMER),
                arguments("0.4000000は三振区間の下端で出塁率と等しい", 0.4f, BattingResult.STRIKEOUT),
                arguments("0.54999995は三振区間の直前", 0.54999995f, BattingResult.STRIKEOUT),
                arguments("0.5500000は凡退区間の下端", 0.55f, BattingResult.BATTED_OUT),
                arguments("1.0000000は凡退区間の上端", 1.0f, BattingResult.BATTED_OUT));
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
            result = sut.batting(ON_BASE_PERCENTAGE, SLUGGING);

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
                        "0.30は長距離で二塁打・中距離で単打",
                        0.30f,
                        BattingResult.HIT_DOUBLE,
                        BattingResult.HIT_SINGLE),
                arguments(
                        "0.34は長距離で本塁打・中距離で二塁打",
                        0.34f,
                        BattingResult.HIT_HOMER,
                        BattingResult.HIT_DOUBLE),
                arguments(
                        "0.36は長距離で本塁打・中距離で三塁打",
                        0.36f,
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
            longResult = sut.batting(ON_BASE_PERCENTAGE, SLUGGING);
            scriptedRandom.assertFullyConsumed();
        }
        BattingResult middleResult;
        try (ScriptedRandom scriptedRandom = ScriptedRandom.of(random)) {
            middleResult = middleDistance.batting(ON_BASE_PERCENTAGE, SLUGGING);
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
                        "既定の配分では0.3500000は本塁打",
                        SimulationRulesTestData.standard().longDistanceHitting(),
                        BattingResult.HIT_HOMER),
                arguments(
                        "本塁打除数を8にすると0.3500000は二塁打",
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
        try (ScriptedRandom scriptedRandom = ScriptedRandom.of(0.35f)) {
            result = sut.batting(ON_BASE_PERCENTAGE, SLUGGING);

            // then
            assertAll(
                    description,
                    () -> assertEquals(expectedResult, result),
                    scriptedRandom::assertFullyConsumed);
        }
    }
}

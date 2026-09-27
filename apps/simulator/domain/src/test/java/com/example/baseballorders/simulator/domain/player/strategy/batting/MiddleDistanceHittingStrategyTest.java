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
 * 中距離打者の確率仕様。
 *
 * <p>出塁率 0.400 / 長打率 0.550 の基準打者での乱数区間は次のとおり。長打分 0.150 を二塁打・三塁打・本塁打へ 1/6 ずつ、単打へ残りを配分する。
 *
 * <pre>
 * [0.000000, 0.050000) 四球
 * [0.050000, 0.334375) 単打
 * [0.334375, 0.356250) 二塁打
 * [0.356250, 0.378125) 三塁打
 * [0.378125, 0.400000) 本塁打
 * [0.400000, 0.550000) 三振
 * [0.550000, 1.000000] 凡退
 * </pre>
 *
 * <p>各区間は下端（含む）と直前（排他）の 2 点を置く。1 区間 1 ケースでは閾値の移動を検出できない。
 */
class MiddleDistanceHittingStrategyTest {

    private static final float ON_BASE_PERCENTAGE = 0.400f;
    private static final float SLUGGING = 0.550f;

    static Stream<Arguments> battingBoundaries() {
        return Stream.of(
                arguments("0.000000は四球区間の下端", 0.0f, BattingResult.WALK),
                arguments("0.049999997は四球区間の直前", 0.049999997f, BattingResult.WALK),
                arguments("0.050000は単打区間の下端", 0.05f, BattingResult.HIT_SINGLE),
                arguments("0.33437496は単打区間の直前", 0.33437496f, BattingResult.HIT_SINGLE),
                arguments("0.334375は二塁打区間の下端", 0.334375f, BattingResult.HIT_DOUBLE),
                arguments("0.35624996は二塁打区間の直前", 0.35624996f, BattingResult.HIT_DOUBLE),
                arguments("0.356250は三塁打区間の下端", 0.35625f, BattingResult.HIT_TRIPLE),
                arguments("0.37812495は三塁打区間の直前", 0.37812495f, BattingResult.HIT_TRIPLE),
                arguments("0.37812498は本塁打区間の下端", 0.37812498f, BattingResult.HIT_HOMER),
                arguments("0.39999998は本塁打区間の直前", 0.39999998f, BattingResult.HIT_HOMER),
                arguments("0.400000は三振区間の下端で出塁率と等しい", 0.4f, BattingResult.STRIKEOUT),
                arguments("0.54999995は三振区間の直前", 0.54999995f, BattingResult.STRIKEOUT),
                arguments("0.550000は凡退区間の下端", 0.55f, BattingResult.BATTED_OUT),
                arguments("1.000000は凡退区間の上端", 1.0f, BattingResult.BATTED_OUT));
    }

    @DisplayName("中距離打者は乱数区間の境界どおりに打席結果を決定し、乱数を1個だけ消費する")
    @ParameterizedTest(name = "{0} -> {2}")
    @MethodSource("battingBoundaries")
    void determinesBattingResultAtBoundary(
            String description, float random, BattingResult expectedResult) {
        // given
        var sut =
                new MiddleDistanceHittingStrategy(
                        SimulationRulesTestData.standard().batting(),
                        SimulationRulesTestData.standard().middleDistanceHitting());

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

    static Stream<Arguments> configuredDistributions() {
        return Stream.of(
                arguments(
                        "既定の配分では0.3700000は三塁打",
                        SimulationRulesTestData.standard().middleDistanceHitting(),
                        BattingResult.HIT_TRIPLE),
                arguments(
                        "本塁打除数を2にすると0.3700000は本塁打",
                        HittingDistributionBuilder.hittingDistribution()
                                .doubleDivisor(6)
                                .tripleDivisor(6)
                                .homeRunDivisor(2)
                                .singleReductionDivisor(2)
                                .build(),
                        BattingResult.HIT_HOMER));
    }

    @DisplayName("中距離打者の長打配分は設定された除数で決まる")
    @ParameterizedTest(name = "{0}")
    @MethodSource("configuredDistributions")
    void usesConfiguredHittingDistribution(
            String description, HittingDistribution distribution, BattingResult expectedResult) {
        // given
        var sut =
                new MiddleDistanceHittingStrategy(
                        SimulationRulesTestData.standard().batting(), distribution);

        // when
        BattingResult result;
        try (ScriptedRandom scriptedRandom = ScriptedRandom.of(0.37f)) {
            result = sut.batting(ON_BASE_PERCENTAGE, SLUGGING);

            // then
            assertAll(
                    description,
                    () -> assertEquals(expectedResult, result),
                    scriptedRandom::assertFullyConsumed);
        }
    }
}

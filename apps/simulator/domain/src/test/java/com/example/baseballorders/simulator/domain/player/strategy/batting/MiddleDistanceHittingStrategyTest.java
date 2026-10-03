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
class MiddleDistanceHittingStrategyTest {

    private static final float BATTING_AVERAGE = 0.400f;

    static Stream<Arguments> battingBoundaries() {
        return Stream.of(
                arguments("乱数0は四球", 0.0f, BattingResult.WALK),
                arguments("0.05の直前", Math.nextDown(0.05f), BattingResult.WALK),
                arguments("0.05と等しい境界", 0.05f, BattingResult.HIT_SINGLE),
                // 安打確率 0.38 を 15:1:1:1 で配分し、単打 0.3667、二塁打 0.3878、三塁打 0.4089 まで。
                arguments("単打上限の直前", 0.3666f, BattingResult.HIT_SINGLE),
                arguments("単打上限の直後", 0.3668f, BattingResult.HIT_DOUBLE),
                arguments("二塁打上限の直前", 0.3877f, BattingResult.HIT_DOUBLE),
                arguments("二塁打上限の直後", 0.3879f, BattingResult.HIT_TRIPLE),
                arguments("三塁打上限の直前", 0.4088f, BattingResult.HIT_TRIPLE),
                arguments("三塁打上限の直後", 0.4090f, BattingResult.HIT_HOMER),
                arguments("0.43の直前", Math.nextDown(0.43f), BattingResult.HIT_HOMER),
                arguments("0.43と等しい境界", 0.43f, BattingResult.STRIKEOUT),
                arguments("0.5725の直前", Math.nextDown(0.5725f), BattingResult.STRIKEOUT),
                arguments("0.5725と等しい境界", 0.5725f, BattingResult.BATTED_OUT),
                arguments("乱数1は凡退", 1.0f, BattingResult.BATTED_OUT));
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
            result = sut.batting(BATTING_AVERAGE);

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
                        "既定の配分では0.3950000は三塁打",
                        SimulationRulesTestData.standard().middleDistanceHitting(),
                        BattingResult.HIT_TRIPLE),
                arguments(
                        "本塁打の重みを15にすると0.3950000は本塁打",
                        HittingDistributionBuilder.hittingDistribution()
                                .singleWeight(15)
                                .doubleWeight(1)
                                .tripleWeight(1)
                                .homeRunWeight(15)
                                .build(),
                        BattingResult.HIT_HOMER),
                arguments(
                        "二塁打と三塁打の重みが0なら0.3950000は本塁打",
                        HittingDistributionBuilder.hittingDistribution()
                                .singleWeight(15)
                                .doubleWeight(0)
                                .tripleWeight(0)
                                .homeRunWeight(15)
                                .build(),
                        BattingResult.HIT_HOMER));
    }

    @DisplayName("中距離打者の長打配分は設定された重みで決まる")
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
        try (ScriptedRandom scriptedRandom = ScriptedRandom.of(0.395f)) {
            result = sut.batting(BATTING_AVERAGE);

            // then
            assertAll(
                    description,
                    () -> assertEquals(expectedResult, result),
                    scriptedRandom::assertFullyConsumed);
        }
    }
}

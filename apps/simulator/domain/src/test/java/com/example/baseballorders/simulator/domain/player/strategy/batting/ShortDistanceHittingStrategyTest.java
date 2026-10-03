package com.example.baseballorders.simulator.domain.player.strategy.batting;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.example.baseballorders.simulator.domain.play.BattingResult;
import com.example.baseballorders.simulator.domain.player.strategy.ScriptedRandom;
import com.example.baseballorders.simulator.domain.rule.BattingProbabilities;
import com.example.baseballorders.simulator.domain.rule.BattingProbabilitiesBuilder;
import com.example.baseballorders.simulator.domain.rule.SimulationRulesTestData;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** 四球を除いた打席に対する打率と、設定された長打配分の確率仕様。 */
class ShortDistanceHittingStrategyTest {

    private static final float BATTING_AVERAGE = 0.400f;
    private static final int UNREACHABLE_SCAN_STEPS = 1000;

    static Stream<Arguments> battingBoundaries() {
        return Stream.of(
                arguments("乱数0は四球", 0.0f, BattingResult.WALK),
                arguments("0.05の直前", Math.nextDown(0.05f), BattingResult.WALK),
                arguments("0.05と等しい境界", 0.05f, BattingResult.HIT_SINGLE),
                // 安打確率 0.95 * 0.4 = 0.38 を 18:2 で配分し、単打区間は 0.05 + 0.342 = 0.392 まで。
                arguments("0.392の直前", 0.3919f, BattingResult.HIT_SINGLE),
                arguments("0.392を超えた直後", 0.3921f, BattingResult.HIT_DOUBLE),
                arguments("0.43の直前", Math.nextDown(0.43f), BattingResult.HIT_DOUBLE),
                arguments("0.43と等しい境界", 0.43f, BattingResult.STRIKEOUT),
                arguments("0.5725の直前", Math.nextDown(0.5725f), BattingResult.STRIKEOUT),
                arguments("0.5725と等しい境界", 0.5725f, BattingResult.BATTED_OUT),
                arguments("乱数1は凡退", 1.0f, BattingResult.BATTED_OUT));
    }

    @DisplayName("短距離打者は乱数区間の境界どおりに打席結果を決定し、乱数を1個だけ消費する")
    @ParameterizedTest(name = "{0} -> {2}")
    @MethodSource("battingBoundaries")
    void determinesBattingResultAtBoundary(
            String description, float random, BattingResult expectedResult) {
        // given
        var sut =
                new ShortDistanceHittingStrategy(
                        SimulationRulesTestData.standard().batting(),
                        SimulationRulesTestData.standard().shortDistanceHitting());

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

    @Test
    @DisplayName("短距離打者は三塁打と本塁打の重みが0のためどの乱数でも到達しない")
    void neverProducesTripleOrHomer() {
        // given
        var sut =
                new ShortDistanceHittingStrategy(
                        SimulationRulesTestData.standard().batting(),
                        SimulationRulesTestData.standard().shortDistanceHitting());
        Set<BattingResult> observed = EnumSet.noneOf(BattingResult.class);
        float[] script = new float[UNREACHABLE_SCAN_STEPS + 1];
        for (int step = 0; step <= UNREACHABLE_SCAN_STEPS; step++) {
            script[step] = (float) step / UNREACHABLE_SCAN_STEPS;
        }

        // when
        try (ScriptedRandom scriptedRandom = ScriptedRandom.of(script)) {
            for (int step = 0; step <= UNREACHABLE_SCAN_STEPS; step++) {
                observed.add(sut.batting(BATTING_AVERAGE));
            }

            // then
            assertAll(
                    () -> assertFalse(observed.contains(BattingResult.HIT_TRIPLE), "三塁打は到達しないこと"),
                    () -> assertFalse(observed.contains(BattingResult.HIT_HOMER), "本塁打は到達しないこと"),
                    () ->
                            assertEquals(
                                    EnumSet.of(
                                            BattingResult.WALK,
                                            BattingResult.HIT_SINGLE,
                                            BattingResult.HIT_DOUBLE,
                                            BattingResult.STRIKEOUT,
                                            BattingResult.BATTED_OUT),
                                    observed,
                                    "到達可能な結果は四球・単打・二塁打・三振・凡退の5種類であること"),
                    () -> scriptedRandom.assertFullyConsumed());
        }
    }

    static Stream<Arguments> configuredBattingProbabilities() {
        return Stream.of(
                arguments(
                        "既定の四球確率0.050では0.1000000は単打",
                        SimulationRulesTestData.standard().batting(),
                        0.1f,
                        BattingResult.HIT_SINGLE),
                arguments(
                        "四球確率を0.250にすると0.1000000は四球",
                        BattingProbabilitiesBuilder.battingProbabilities()
                                .walkProbability(0.25f)
                                .strikeoutProbabilityWhenNotOnBase(0.25f)
                                .build(),
                        0.1f,
                        BattingResult.WALK),
                arguments(
                        "既定の三振割合0.250では0.6000000は凡退",
                        SimulationRulesTestData.standard().batting(),
                        0.6f,
                        BattingResult.BATTED_OUT),
                arguments(
                        "三振割合を0.500にすると0.6000000は三振",
                        BattingProbabilitiesBuilder.battingProbabilities()
                                .walkProbability(0.05f)
                                .strikeoutProbabilityWhenNotOnBase(0.5f)
                                .build(),
                        0.6f,
                        BattingResult.STRIKEOUT));
    }

    @DisplayName("四球と三振の区間は設定された打席確率で決まる")
    @ParameterizedTest(name = "{0}")
    @MethodSource("configuredBattingProbabilities")
    void usesConfiguredBattingProbabilities(
            String description,
            BattingProbabilities probabilities,
            float random,
            BattingResult expectedResult) {
        // given
        var sut =
                new ShortDistanceHittingStrategy(
                        probabilities, SimulationRulesTestData.standard().shortDistanceHitting());

        // when
        BattingResult result;
        try (ScriptedRandom scriptedRandom = ScriptedRandom.of(random)) {
            result = sut.batting(BATTING_AVERAGE);

            // then
            assertAll(
                    description,
                    () -> assertEquals(expectedResult, result),
                    scriptedRandom::assertFullyConsumed);
        }
    }
}

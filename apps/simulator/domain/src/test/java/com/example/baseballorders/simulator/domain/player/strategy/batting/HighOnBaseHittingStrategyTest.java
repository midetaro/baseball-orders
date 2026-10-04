package com.example.baseballorders.simulator.domain.player.strategy.batting;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.example.baseballorders.simulator.domain.play.BattingResult;
import com.example.baseballorders.simulator.domain.player.strategy.ScriptedRandom;
import com.example.baseballorders.simulator.domain.rule.SimulationRulesTestData;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** 高出塁率打者は打率を変えず、四球確率だけを専用の設定値にする確率仕様。 */
class HighOnBaseHittingStrategyTest {

    private static final float BATTING_AVERAGE = 0.400f;

    static Stream<Arguments> battingResults() {
        // 四球0.1、安打確率は(1 - 0.1) * 0.4 = 0.36、出塁確率は0.46。
        // 高出塁率打者の重み18:2:0:0で単打0.424、二塁打0.46、三振0.595までが各区間。
        return Stream.of(
                arguments("乱数0は四球", 0.0f, BattingResult.WALK),
                arguments("0.1の直前は四球", Math.nextDown(0.1f), BattingResult.WALK),
                arguments("0.1と等しい境界は単打", 0.1f, BattingResult.HIT_SINGLE),
                arguments("単打上限の直前", 0.4239f, BattingResult.HIT_SINGLE),
                arguments("単打上限の直後", 0.4241f, BattingResult.HIT_DOUBLE),
                arguments("二塁打上限の直前", 0.4599f, BattingResult.HIT_DOUBLE),
                arguments("二塁打上限の直後は三振", 0.4601f, BattingResult.STRIKEOUT),
                arguments("三振区間の内側", 0.50f, BattingResult.STRIKEOUT),
                arguments("乱数1は凡退", 1.0f, BattingResult.BATTED_OUT));
    }

    @DisplayName("高出塁率打者は四球確率0.1と専用の安打配分で打席結果を決定し、乱数を1個だけ消費する")
    @ParameterizedTest(name = "{0} -> {2}")
    @MethodSource("battingResults")
    void determinesBattingResult(String description, float random, BattingResult expectedResult) {
        // given
        var sut = SimulationRulesTestData.strategies().highOnBaseHittingStrategy();

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
    @DisplayName("標準の四球確率では単打になる乱数でも、高出塁率打者は四球を選ぶ")
    void walksMoreOftenThanMiddleDistanceHitter() {
        // given
        var strategies = SimulationRulesTestData.strategies();
        var middleDistance = strategies.middleDistanceHittingStrategy();
        var sut = strategies.highOnBaseHittingStrategy();

        // when
        BattingResult middleDistanceResult;
        BattingResult highOnBaseResult;
        try (ScriptedRandom scriptedRandom = ScriptedRandom.of(0.07f, 0.07f)) {
            middleDistanceResult = middleDistance.batting(BATTING_AVERAGE);
            highOnBaseResult = sut.batting(BATTING_AVERAGE);
            scriptedRandom.assertFullyConsumed();
        }

        // then
        assertAll(
                () -> assertEquals(BattingResult.HIT_SINGLE, middleDistanceResult),
                () -> assertEquals(BattingResult.WALK, highOnBaseResult));
    }
}

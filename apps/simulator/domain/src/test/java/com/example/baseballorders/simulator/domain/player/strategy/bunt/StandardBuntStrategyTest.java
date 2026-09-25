package com.example.baseballorders.simulator.domain.player.strategy.bunt;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.example.baseballorders.simulator.domain.play.BuntResult;
import com.example.baseballorders.simulator.domain.play.OutCount;
import com.example.baseballorders.simulator.domain.player.strategy.ScriptedRandom;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * 標準バント戦略の確率仕様。
 *
 * <p>試行するのは無死のときだけで、一死以降は乱数を引かずに試行しない。成功率 0.700 では次のとおり。
 *
 * <pre>
 * 無死  : [0.0000000, 0.7000000) 成功 / [0.7000000, 1.0000000] 失敗
 * 一死  : 乱数を引かず NOT_TRY
 * 二死  : 乱数を引かず NOT_TRY
 * 三死  : 乱数を引かず NOT_TRY
 * </pre>
 */
class StandardBuntStrategyTest {

    private static final float SUCCESS_RATE = 0.700f;

    static Stream<Arguments> buntBoundaries() {
        return Stream.of(
                arguments("0.0000000は成功区間の下端", 0.0f, BuntResult.SUCCESS),
                arguments("0.6999999は成功区間の直前", 0.6999999f, BuntResult.SUCCESS),
                arguments("0.7000000は成功率と等しく失敗区間の下端", 0.7f, BuntResult.FAILURE),
                arguments("1.0000000は失敗区間の上端", 1.0f, BuntResult.FAILURE));
    }

    @DisplayName("無死では乱数区間の境界どおりにバント結果を決定し、乱数を1個だけ消費する")
    @ParameterizedTest(name = "{0} -> {2}")
    @MethodSource("buntBoundaries")
    void determinesBuntResultAtBoundaryWithNoOut(
            String description, float random, BuntResult expectedResult) {
        // given
        var sut = new StandardBuntStrategy();

        // when
        BuntResult result;
        try (ScriptedRandom scriptedRandom = ScriptedRandom.of(random)) {
            result = sut.bunt(SUCCESS_RATE, OutCount.NO_OUT);

            // then
            assertAll(
                    description,
                    () -> assertEquals(expectedResult, result),
                    () -> assertEquals(1, scriptedRandom.consumedCount()),
                    () -> scriptedRandom.assertFullyConsumed());
        }
    }

    @DisplayName("一死以降は乱数を引かずにバントを試行しない")
    @ParameterizedTest
    @EnumSource(
            value = OutCount.class,
            names = {"ONE_OUT", "TWO_OUT", "THREE_OUT"})
    void neverAttemptsBuntAfterFirstOut(OutCount outCount) {
        // given
        var sut = new StandardBuntStrategy();

        // when
        BuntResult result;
        try (ScriptedRandom scriptedRandom = ScriptedRandom.of()) {
            result = sut.bunt(SUCCESS_RATE, outCount);

            // then
            assertAll(
                    () -> assertEquals(BuntResult.NOT_TRY, result),
                    () -> assertEquals(0, scriptedRandom.consumedCount()),
                    () -> scriptedRandom.assertFullyConsumed());
        }
    }
}

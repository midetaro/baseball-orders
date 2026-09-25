package com.example.baseballorders.simulator.domain.player.strategy.steal;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.example.baseballorders.simulator.domain.play.StealResult;
import com.example.baseballorders.simulator.domain.player.strategy.ScriptedRandom;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * 積極盗塁戦略の確率仕様。
 *
 * <p>成功率 0.800 の走者での乱数区間は次のとおり。試行率は二盗 30% / 三盗 15% で、標準戦略より試行区間が広い。
 *
 * <pre>
 * 二盗 : [0.0000000, 0.7000000) 試行しない
 *        (0.7000000, 0.9400000) 成功
 *        その他                 失敗（0.7000000 ちょうどを含む）
 * 三盗 : [0.0000000, 0.8500000) 試行しない
 *        (0.8500000, 0.9700000) 成功
 *        その他                 失敗（0.8500000 ちょうどを含む）
 * </pre>
 *
 * <p>標準戦略と同様に {@code r} がちょうど試行境界と等しいときは失敗になる。
 */
class EagerStealStrategyTest {

    private static final float STEAL_SUCCESS_RATE = 0.800f;

    static Stream<Arguments> stealToSecondBoundaries() {
        return Stream.of(
                arguments("0.0000000は試行しない区間の下端", 0.0f, StealResult.NOT_TRY),
                arguments("0.6999999は試行しない区間の直前", 0.6999999f, StealResult.NOT_TRY),
                arguments("0.7000000は試行境界と等しく失敗になる", 0.7f, StealResult.FAILURE),
                arguments("0.70000005は成功区間の下端", 0.70000005f, StealResult.SUCCESS),
                arguments("0.93999994は成功区間の直前", 0.93999994f, StealResult.SUCCESS),
                arguments("0.9400000は成功上限と等しく失敗になる", 0.94f, StealResult.FAILURE),
                arguments("1.0000000は失敗区間の上端", 1.0f, StealResult.FAILURE));
    }

    static Stream<Arguments> stealToThirdBoundaries() {
        return Stream.of(
                arguments("0.0000000は試行しない区間の下端", 0.0f, StealResult.NOT_TRY),
                arguments("0.84999996は試行しない区間の直前", 0.84999996f, StealResult.NOT_TRY),
                arguments("0.8500000は試行境界と等しく失敗になる", 0.85f, StealResult.FAILURE),
                arguments("0.8500001は成功区間の下端", 0.8500001f, StealResult.SUCCESS),
                arguments("0.96999997は成功区間の直前", 0.96999997f, StealResult.SUCCESS),
                arguments("0.9700000は成功上限と等しく失敗になる", 0.97f, StealResult.FAILURE),
                arguments("1.0000000は失敗区間の上端", 1.0f, StealResult.FAILURE));
    }

    static Stream<Arguments> standardStrategyComparisons() {
        return Stream.of(
                arguments("0.7500000の二盗は積極戦略だけが試行する", 0.75f, true),
                arguments("0.9000000の三盗は積極戦略だけが試行する", 0.90f, false));
    }

    @DisplayName("積極戦略は二盗の乱数区間の境界どおりに結果を決定し、乱数を1個だけ消費する")
    @ParameterizedTest(name = "{0} -> {2}")
    @MethodSource("stealToSecondBoundaries")
    void determinesStealToSecondAtBoundary(
            String description, float random, StealResult expectedResult) {
        // given
        var sut = new EagerStealStrategy();

        // when
        StealResult result;
        try (ScriptedRandom scriptedRandom = ScriptedRandom.of(random)) {
            result = sut.runToDouble(STEAL_SUCCESS_RATE);

            // then
            assertAll(
                    description,
                    () -> assertEquals(expectedResult, result),
                    () -> assertEquals(1, scriptedRandom.consumedCount()),
                    () -> scriptedRandom.assertFullyConsumed());
        }
    }

    @DisplayName("積極戦略は三盗の乱数区間の境界どおりに結果を決定し、乱数を1個だけ消費する")
    @ParameterizedTest(name = "{0} -> {2}")
    @MethodSource("stealToThirdBoundaries")
    void determinesStealToThirdAtBoundary(
            String description, float random, StealResult expectedResult) {
        // given
        var sut = new EagerStealStrategy();

        // when
        StealResult result;
        try (ScriptedRandom scriptedRandom = ScriptedRandom.of(random)) {
            result = sut.runToTriple(STEAL_SUCCESS_RATE);

            // then
            assertAll(
                    description,
                    () -> assertEquals(expectedResult, result),
                    () -> assertEquals(1, scriptedRandom.consumedCount()),
                    () -> scriptedRandom.assertFullyConsumed());
        }
    }

    @DisplayName("標準戦略が試行しない乱数でも積極戦略は盗塁を試みる")
    @ParameterizedTest(name = "{0}")
    @MethodSource("standardStrategyComparisons")
    void attemptsStealWhereStandardStrategyDoesNot(
            String description, float random, boolean toSecond) {
        // given
        var sut = new EagerStealStrategy();
        var standardStrategy = new StandardStealStrategy();

        // when
        StealResult eagerResult;
        try (ScriptedRandom scriptedRandom = ScriptedRandom.of(random)) {
            eagerResult =
                    toSecond
                            ? sut.runToDouble(STEAL_SUCCESS_RATE)
                            : sut.runToTriple(STEAL_SUCCESS_RATE);
            scriptedRandom.assertFullyConsumed();
        }
        StealResult standardResult;
        try (ScriptedRandom scriptedRandom = ScriptedRandom.of(random)) {
            standardResult =
                    toSecond
                            ? standardStrategy.runToDouble(STEAL_SUCCESS_RATE)
                            : standardStrategy.runToTriple(STEAL_SUCCESS_RATE);
            scriptedRandom.assertFullyConsumed();
        }

        // then
        assertAll(
                description,
                () -> assertEquals(StealResult.SUCCESS, eagerResult, "積極戦略は試行して成功すること"),
                () -> assertEquals(StealResult.NOT_TRY, standardResult, "標準戦略は試行しないこと"));
    }
}

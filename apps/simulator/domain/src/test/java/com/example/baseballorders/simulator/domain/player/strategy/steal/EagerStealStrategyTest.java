package com.example.baseballorders.simulator.domain.player.strategy.steal;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.example.baseballorders.simulator.domain.play.StealResult;
import com.example.baseballorders.simulator.domain.player.strategy.ScriptedRandom;
import com.example.baseballorders.simulator.domain.rule.StealAttemptRates;
import com.example.baseballorders.simulator.domain.rule.StealAttemptRatesBuilder;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * 積極盗塁戦略の確率仕様。
 *
 * <p>設定された盗塁成功率 0.700・企図率（二盗 30% / 三盗 15%）での乱数区間は次のとおり。二盗の企図率は標準戦略と同じ 30% だが、 三盗の企図率は標準戦略の 10% より広い
 * 15% になる。
 *
 * <pre>
 * 二盗 : [0.0000000, 0.7000000) 試行しない
 *        (0.7000000, 0.9100000) 成功
 *        その他                 失敗（0.7000000 ちょうどを含む）
 * 三盗 : [0.0000000, 0.8500000) 試行しない
 *        (0.8500000, 0.9550000) 成功
 *        その他                 失敗（0.8500000 ちょうどを含む）
 * </pre>
 *
 * <p>標準戦略と同様に {@code r} がちょうど試行境界と等しいときは失敗になる。
 */
class EagerStealStrategyTest {

    private static final StealAttemptRates ATTEMPT_RATES =
            StealAttemptRatesBuilder.stealAttemptRates()
                    .toDoubleAttemptRate(0.3f)
                    .toTripleAttemptRate(0.15f)
                    .build();

    private static final float STEAL_SUCCESS_RATE = 0.700f;

    static Stream<Arguments> stealToSecondBoundaries() {
        return Stream.of(
                arguments("0.0000000は試行しない区間の下端", 0.0f, StealResult.NOT_TRY),
                arguments("0.6999999は試行しない区間の直前", 0.6999999f, StealResult.NOT_TRY),
                arguments("0.7000000は試行境界と等しく失敗になる", 0.7f, StealResult.FAILURE),
                arguments("0.7000001は成功区間の下端", 0.7000001f, StealResult.SUCCESS),
                arguments("0.9099999は成功区間の直前", 0.9099999f, StealResult.SUCCESS),
                arguments("0.9100000は成功上限と等しく失敗になる", 0.91f, StealResult.FAILURE),
                arguments("1.0000000は失敗区間の上端", 1.0f, StealResult.FAILURE));
    }

    static Stream<Arguments> stealToThirdBoundaries() {
        return Stream.of(
                arguments("0.0000000は試行しない区間の下端", 0.0f, StealResult.NOT_TRY),
                arguments("0.8499999は試行しない区間の直前", 0.8499999f, StealResult.NOT_TRY),
                arguments("0.8500000は試行境界と等しく失敗になる", 0.85f, StealResult.FAILURE),
                arguments("0.8500001は成功区間の下端", 0.8500001f, StealResult.SUCCESS),
                arguments("0.9549999は成功区間の直前", 0.9549999f, StealResult.SUCCESS),
                arguments("0.9550000は成功上限と等しく失敗になる", 0.95500005f, StealResult.FAILURE),
                arguments("1.0000000は失敗区間の上端", 1.0f, StealResult.FAILURE));
    }

    @DisplayName("積極戦略は二盗の乱数区間の境界どおりに結果を決定し、乱数を1個だけ消費する")
    @ParameterizedTest(name = "{0} -> {2}")
    @MethodSource("stealToSecondBoundaries")
    void determinesStealToSecondAtBoundary(
            String description, float random, StealResult expectedResult) {
        // given
        var sut = new EagerStealStrategy(ATTEMPT_RATES, STEAL_SUCCESS_RATE);

        // when
        StealResult result;
        try (ScriptedRandom scriptedRandom = ScriptedRandom.of(random)) {
            result = sut.runToDouble();

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
        var sut = new EagerStealStrategy(ATTEMPT_RATES, STEAL_SUCCESS_RATE);

        // when
        StealResult result;
        try (ScriptedRandom scriptedRandom = ScriptedRandom.of(random)) {
            result = sut.runToTriple();

            // then
            assertAll(
                    description,
                    () -> assertEquals(expectedResult, result),
                    () -> assertEquals(1, scriptedRandom.consumedCount()),
                    () -> scriptedRandom.assertFullyConsumed());
        }
    }

    @DisplayName("三盗では標準戦略が試行しない乱数でも積極戦略は盗塁を試みる")
    @org.junit.jupiter.api.Test
    void attemptsStealToThirdWhereStandardStrategyDoesNot() {
        // given
        var eager = new EagerStealStrategy(ATTEMPT_RATES, STEAL_SUCCESS_RATE);
        var standardAttemptRates =
                StealAttemptRatesBuilder.stealAttemptRates()
                        .toDoubleAttemptRate(0.3f)
                        .toTripleAttemptRate(0.10f)
                        .build();
        var standard = new StandardStealStrategy(standardAttemptRates, STEAL_SUCCESS_RATE);
        float random = 0.87f;

        // when
        StealResult eagerResult;
        try (ScriptedRandom scriptedRandom = ScriptedRandom.of(random)) {
            eagerResult = eager.runToTriple();
            scriptedRandom.assertFullyConsumed();
        }
        StealResult standardResult;
        try (ScriptedRandom scriptedRandom = ScriptedRandom.of(random)) {
            standardResult = standard.runToTriple();
            scriptedRandom.assertFullyConsumed();
        }

        // then
        assertAll(
                () -> assertEquals(StealResult.SUCCESS, eagerResult, "積極戦略は試行して成功すること"),
                () -> assertEquals(StealResult.NOT_TRY, standardResult, "標準戦略は試行しないこと"));
    }

    static Stream<Arguments> configuredAttemptRates() {
        return Stream.of(
                arguments("設定値0.300では0.6000000は試行しない", ATTEMPT_RATES, StealResult.NOT_TRY),
                arguments(
                        "設定値0.700では0.6000000は成功する",
                        StealAttemptRatesBuilder.stealAttemptRates()
                                .toDoubleAttemptRate(0.7f)
                                .toTripleAttemptRate(0.15f)
                                .build(),
                        StealResult.SUCCESS));
    }

    @DisplayName("積極戦略の二盗試行境界は設定された企図率で決まる")
    @ParameterizedTest(name = "{0}")
    @MethodSource("configuredAttemptRates")
    void usesConfiguredAttemptRateForSecond(
            String description, StealAttemptRates attemptRates, StealResult expectedResult) {
        // given
        var sut = new EagerStealStrategy(attemptRates, STEAL_SUCCESS_RATE);

        // when
        StealResult result;
        try (ScriptedRandom scriptedRandom = ScriptedRandom.of(0.6f)) {
            result = sut.runToDouble();

            // then
            assertAll(
                    description,
                    () -> assertEquals(expectedResult, result),
                    scriptedRandom::assertFullyConsumed);
        }
    }
}

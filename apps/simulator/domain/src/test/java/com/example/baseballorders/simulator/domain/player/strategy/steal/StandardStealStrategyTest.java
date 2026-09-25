package com.example.baseballorders.simulator.domain.player.strategy.steal;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.example.baseballorders.simulator.domain.play.StealResult;
import com.example.baseballorders.simulator.domain.player.strategy.ScriptedRandom;
import com.example.baseballorders.simulator.domain.rule.SimulationRulesTestData;
import com.example.baseballorders.simulator.domain.rule.StealAttemptRates;
import com.example.baseballorders.simulator.domain.rule.StealAttemptRatesBuilder;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * 標準盗塁戦略の確率仕様。
 *
 * <p>成功率 0.800 の走者での乱数区間は次のとおり。試行率は二盗 20% / 三盗 5%。
 *
 * <pre>
 * 二盗 : [0.0000000, 0.8000000) 試行しない
 *        (0.8000000, 0.9600000) 成功
 *        その他                 失敗（0.8000000 ちょうどを含む）
 * 三盗 : [0.0000000, 0.9500000) 試行しない
 *        (0.9500000, 0.9900000) 成功
 *        その他                 失敗（0.9500000 ちょうどを含む）
 * </pre>
 *
 * <p>実装が {@code if (r < NOT_TRY) ... else if (NOT_TRY < r && r < successProbability) ...}
 * という構造のため、{@code r} がちょうど試行境界と等しいときは試行しないのではなく失敗になる。この「角」を境界値として固定する。
 */
class StandardStealStrategyTest {

    private static final float STEAL_SUCCESS_RATE = 0.800f;

    static Stream<Arguments> stealToSecondBoundaries() {
        return Stream.of(
                arguments("0.0000000は試行しない区間の下端", 0.0f, StealResult.NOT_TRY),
                arguments("0.79999995は試行しない区間の直前", 0.79999995f, StealResult.NOT_TRY),
                arguments("0.8000000は試行境界と等しく失敗になる", 0.8f, StealResult.FAILURE),
                arguments("0.8000001は成功区間の下端", 0.8000001f, StealResult.SUCCESS),
                arguments("0.9600000は成功区間の直前", 0.96f, StealResult.SUCCESS),
                arguments("0.96000004は成功上限と等しく失敗になる", 0.96000004f, StealResult.FAILURE),
                arguments("1.0000000は失敗区間の上端", 1.0f, StealResult.FAILURE));
    }

    static Stream<Arguments> stealToThirdBoundaries() {
        return Stream.of(
                arguments("0.0000000は試行しない区間の下端", 0.0f, StealResult.NOT_TRY),
                arguments("0.9499999は試行しない区間の直前", 0.9499999f, StealResult.NOT_TRY),
                arguments("0.9500000は試行境界と等しく失敗になる", 0.95f, StealResult.FAILURE),
                arguments("0.95000005は成功区間の下端", 0.95000005f, StealResult.SUCCESS),
                arguments("0.98999995は成功区間の直前", 0.98999995f, StealResult.SUCCESS),
                arguments("0.9900000は成功上限と等しく失敗になる", 0.99f, StealResult.FAILURE),
                arguments("1.0000000は失敗区間の上端", 1.0f, StealResult.FAILURE));
    }

    static Stream<Arguments> successRateComparisons() {
        return Stream.of(
                arguments("成功率0.800なら0.9500000は成功範囲内", 0.800f, StealResult.SUCCESS),
                arguments("成功率0.700なら0.9500000は成功上限を超える", 0.700f, StealResult.FAILURE));
    }

    @DisplayName("標準戦略は二盗の乱数区間の境界どおりに結果を決定し、乱数を1個だけ消費する")
    @ParameterizedTest(name = "{0} -> {2}")
    @MethodSource("stealToSecondBoundaries")
    void determinesStealToSecondAtBoundary(
            String description, float random, StealResult expectedResult) {
        // given
        var sut = new StandardStealStrategy(SimulationRulesTestData.standard().standardSteal());

        // when
        StealResult result;
        try (ScriptedRandom scriptedRandom = ScriptedRandom.of(random)) {
            result = sut.runToDouble(STEAL_SUCCESS_RATE);

            // then
            assertAll(
                    description,
                    () -> assertEquals(expectedResult, result),
                    () -> assertEquals(1, scriptedRandom.consumedCount()),
                    scriptedRandom::assertFullyConsumed);
        }
    }

    @DisplayName("標準戦略は三盗の乱数区間の境界どおりに結果を決定し、乱数を1個だけ消費する")
    @ParameterizedTest(name = "{0} -> {2}")
    @MethodSource("stealToThirdBoundaries")
    void determinesStealToThirdAtBoundary(
            String description, float random, StealResult expectedResult) {
        // given
        var sut = new StandardStealStrategy(SimulationRulesTestData.standard().standardSteal());

        // when
        StealResult result;
        try (ScriptedRandom scriptedRandom = ScriptedRandom.of(random)) {
            result = sut.runToTriple(STEAL_SUCCESS_RATE);

            // then
            assertAll(
                    description,
                    () -> assertEquals(expectedResult, result),
                    () -> assertEquals(1, scriptedRandom.consumedCount()),
                    scriptedRandom::assertFullyConsumed);
        }
    }

    @DisplayName("標準戦略の二盗成功上限は走者本人の盗塁成功率で決まる")
    @ParameterizedTest(name = "{0}")
    @MethodSource("successRateComparisons")
    void usesRunnerStealSuccessRateForSecond(
            String description, float stealSuccessRate, StealResult expectedResult) {
        // given
        var sut = new StandardStealStrategy(SimulationRulesTestData.standard().standardSteal());

        // when
        StealResult result;
        try (ScriptedRandom scriptedRandom = ScriptedRandom.of(0.95f)) {
            result = sut.runToDouble(stealSuccessRate);

            // then
            assertAll(
                    description,
                    () -> assertEquals(expectedResult, result),
                    scriptedRandom::assertFullyConsumed);
        }
    }

    static Stream<Arguments> configuredAttemptRates() {
        return Stream.of(
                arguments(
                        "設定値0.200では0.6000000は試行しない",
                        SimulationRulesTestData.standard().standardSteal(),
                        StealResult.NOT_TRY),
                arguments(
                        "設定値0.500では0.6000000は成功する",
                        StealAttemptRatesBuilder.stealAttemptRates()
                                .toDoubleAttemptRate(0.5f)
                                .toTripleAttemptRate(0.05f)
                                .build(),
                        StealResult.SUCCESS));
    }

    @DisplayName("標準戦略の二盗試行境界は設定された企図率で決まる")
    @ParameterizedTest(name = "{0}")
    @MethodSource("configuredAttemptRates")
    void usesConfiguredAttemptRateForSecond(
            String description, StealAttemptRates attemptRates, StealResult expectedResult) {
        // given
        var sut = new StandardStealStrategy(attemptRates);

        // when
        StealResult result;
        try (ScriptedRandom scriptedRandom = ScriptedRandom.of(0.6f)) {
            result = sut.runToDouble(STEAL_SUCCESS_RATE);

            // then
            assertAll(
                    description,
                    () -> assertEquals(expectedResult, result),
                    scriptedRandom::assertFullyConsumed);
        }
    }
}

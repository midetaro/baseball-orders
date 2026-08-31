package com.example.baseballorders.simulator.domain.model.behavior;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mockStatic;

import com.example.baseballorders.simulator.domain.code.StealResult;
import com.example.baseballorders.simulator.domain.util.RandomGenerator;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

class StealBehaviorTest {

    @DisplayName("盗塁戦略は乱数の境界に応じて盗塁結果を決定する")
    @ParameterizedTest(name = "{0}")
    @MethodSource("randomStealTestCases")
    void determinesStealResultFromRandomValue(
            String description,
            StealStrategy strategy,
            Destination destination,
            float random,
            StealResult expectedResult) {
        // given
        try (MockedStatic<RandomGenerator> randomGenerator = mockStatic(RandomGenerator.class)) {
            randomGenerator.when(RandomGenerator::nextFloat).thenReturn(random);

            // when
            StealResult result = destination.run(strategy);

            // then
            assertAll(() -> assertEquals(expectedResult, result, description));
        }
    }

    static Stream<Arguments> randomStealTestCases() {
        return Stream.of(
                arguments(
                        "積極的戦略で二塁への試行確率未満なら試行しない",
                        new AggressiveStealBehavior(),
                        Destination.SECOND,
                        0.69f,
                        StealResult.NOT_TRY),
                arguments(
                        "積極的戦略で二塁への試行境界と等しければ失敗する",
                        new AggressiveStealBehavior(),
                        Destination.SECOND,
                        0.7f,
                        StealResult.FAILURE),
                arguments(
                        "積極的戦略で二塁への成功範囲内なら成功する",
                        new AggressiveStealBehavior(),
                        Destination.SECOND,
                        0.8f,
                        StealResult.SUCCESS),
                arguments(
                        "積極的戦略で二塁への成功上限と等しければ失敗する",
                        new AggressiveStealBehavior(),
                        Destination.SECOND,
                        0.97f,
                        StealResult.FAILURE),
                arguments(
                        "積極的戦略で三塁への試行確率未満なら試行しない",
                        new AggressiveStealBehavior(),
                        Destination.THIRD,
                        0.84f,
                        StealResult.NOT_TRY),
                arguments(
                        "積極的戦略で三塁への試行境界と等しければ失敗する",
                        new AggressiveStealBehavior(),
                        Destination.THIRD,
                        0.85f,
                        StealResult.FAILURE),
                arguments(
                        "積極的戦略で三塁への成功範囲内なら成功する",
                        new AggressiveStealBehavior(),
                        Destination.THIRD,
                        0.9f,
                        StealResult.SUCCESS),
                arguments(
                        "積極的戦略で三塁への成功上限と等しければ失敗する",
                        new AggressiveStealBehavior(),
                        Destination.THIRD,
                        0.985f,
                        StealResult.FAILURE),
                arguments(
                        "標準戦略で二塁への試行確率未満なら試行しない",
                        new MiddleStealBehavior(),
                        Destination.SECOND,
                        0.79f,
                        StealResult.NOT_TRY),
                arguments(
                        "標準戦略で二塁への試行境界と等しければ失敗する",
                        new MiddleStealBehavior(),
                        Destination.SECOND,
                        0.8f,
                        StealResult.FAILURE),
                arguments(
                        "標準戦略で二塁への成功範囲内なら成功する",
                        new MiddleStealBehavior(),
                        Destination.SECOND,
                        0.9f,
                        StealResult.SUCCESS),
                arguments(
                        "標準戦略で二塁への成功上限と等しければ失敗する",
                        new MiddleStealBehavior(),
                        Destination.SECOND,
                        0.8f + 0.8f * 0.2f,
                        StealResult.FAILURE),
                arguments(
                        "標準戦略で三塁への試行確率未満なら試行しない",
                        new MiddleStealBehavior(),
                        Destination.THIRD,
                        0.94f,
                        StealResult.NOT_TRY),
                arguments(
                        "標準戦略で三塁への試行境界と等しければ失敗する",
                        new MiddleStealBehavior(),
                        Destination.THIRD,
                        0.95f,
                        StealResult.FAILURE),
                arguments(
                        "標準戦略で三塁への成功範囲内なら成功する",
                        new MiddleStealBehavior(),
                        Destination.THIRD,
                        0.97f,
                        StealResult.SUCCESS),
                arguments(
                        "標準戦略で三塁への成功上限と等しければ失敗する",
                        new MiddleStealBehavior(),
                        Destination.THIRD,
                        0.995f,
                        StealResult.FAILURE));
    }

    @DisplayName("盗塁しない戦略は進塁先にかかわらず試行しない")
    @ParameterizedTest(name = "{0}")
    @MethodSource("nowayStealTestCases")
    void neverAttemptsSteal(
            String description, Destination destination, StealResult expectedResult) {
        // given
        var strategy = new NowayStealBehavior();

        // when
        StealResult result = destination.run(strategy);

        // then
        assertAll(() -> assertEquals(expectedResult, result, description));
    }

    static Stream<Arguments> nowayStealTestCases() {
        return Stream.of(
                arguments("二塁へは盗塁を試行しない", Destination.SECOND, StealResult.NOT_TRY),
                arguments("三塁へは盗塁を試行しない", Destination.THIRD, StealResult.NOT_TRY));
    }

    enum Destination {
        SECOND {
            @Override
            StealResult run(StealStrategy strategy) {
                return strategy.runToDouble();
            }
        },
        THIRD {
            @Override
            StealResult run(StealStrategy strategy) {
                return strategy.runToTriple();
            }
        };

        abstract StealResult run(StealStrategy strategy);
    }
}

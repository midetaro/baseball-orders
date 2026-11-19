package com.example.baseballorders.simulator.domain.player.strategy.steal;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mockStatic;

import com.example.baseballorders.simulator.domain.play.StealResult;
import com.example.baseballorders.simulator.domain.player.strategy.RandomGenerator;
import com.example.baseballorders.simulator.domain.rule.SimulationRulesTestData;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

class StealBehaviorTest {

    static Stream<Arguments> randomStealTestCases() {
        return Stream.of(
                arguments(
                        "積極的戦略で二塁への試行確率未満なら試行しない",
                        SimulationRulesTestData.strategies().eagerSteal(),
                        Destination.SECOND,
                        0.69f,
                        StealResult.NOT_TRY),
                arguments(
                        "積極的戦略で二塁への試行境界と等しければ失敗する",
                        SimulationRulesTestData.strategies().eagerSteal(),
                        Destination.SECOND,
                        0.7f,
                        StealResult.FAILURE),
                arguments(
                        "積極的戦略で二塁への成功範囲内なら成功する",
                        SimulationRulesTestData.strategies().eagerSteal(),
                        Destination.SECOND,
                        0.8f,
                        StealResult.SUCCESS),
                arguments(
                        "積極的戦略で二塁への成功上限と等しければ失敗する",
                        SimulationRulesTestData.strategies().eagerSteal(),
                        Destination.SECOND,
                        0.91f,
                        StealResult.FAILURE),
                arguments(
                        "積極的戦略で三塁への試行確率未満なら試行しない",
                        SimulationRulesTestData.strategies().eagerSteal(),
                        Destination.THIRD,
                        0.84f,
                        StealResult.NOT_TRY),
                arguments(
                        "積極的戦略で三塁への試行境界と等しければ失敗する",
                        SimulationRulesTestData.strategies().eagerSteal(),
                        Destination.THIRD,
                        0.85f,
                        StealResult.FAILURE),
                arguments(
                        "積極的戦略で三塁への成功範囲内なら成功する",
                        SimulationRulesTestData.strategies().eagerSteal(),
                        Destination.THIRD,
                        0.9f,
                        StealResult.SUCCESS),
                arguments(
                        "積極的戦略で三塁への成功上限と等しければ失敗する",
                        SimulationRulesTestData.strategies().eagerSteal(),
                        Destination.THIRD,
                        0.95500005f,
                        StealResult.FAILURE),
                arguments(
                        "標準戦略で二塁への試行確率未満なら試行しない",
                        SimulationRulesTestData.strategies().standardSteal(),
                        Destination.SECOND,
                        0.69f,
                        StealResult.NOT_TRY),
                arguments(
                        "標準戦略で二塁への試行境界と等しければ失敗する",
                        SimulationRulesTestData.strategies().standardSteal(),
                        Destination.SECOND,
                        0.7f,
                        StealResult.FAILURE),
                arguments(
                        "標準戦略で二塁への成功範囲内なら成功する",
                        SimulationRulesTestData.strategies().standardSteal(),
                        Destination.SECOND,
                        0.8f,
                        StealResult.SUCCESS),
                arguments(
                        "標準戦略で二塁への成功上限と等しければ失敗する",
                        SimulationRulesTestData.strategies().standardSteal(),
                        Destination.SECOND,
                        0.91f,
                        StealResult.FAILURE),
                arguments(
                        "標準戦略で三塁への試行確率未満なら試行しない",
                        SimulationRulesTestData.strategies().standardSteal(),
                        Destination.THIRD,
                        0.89f,
                        StealResult.NOT_TRY),
                arguments(
                        "標準戦略で三塁への試行境界と等しければ失敗する",
                        SimulationRulesTestData.strategies().standardSteal(),
                        Destination.THIRD,
                        0.9f,
                        StealResult.FAILURE),
                arguments(
                        "標準戦略で三塁への成功範囲内なら成功する",
                        SimulationRulesTestData.strategies().standardSteal(),
                        Destination.THIRD,
                        0.95f,
                        StealResult.SUCCESS),
                arguments(
                        "標準戦略で三塁への成功上限と等しければ失敗する",
                        SimulationRulesTestData.strategies().standardSteal(),
                        Destination.THIRD,
                        0.97f,
                        StealResult.FAILURE));
    }

    static Stream<Arguments> nowayStealTestCases() {
        return Stream.of(
                arguments("二塁へは盗塁を試行しない", Destination.SECOND, StealResult.NOT_TRY),
                arguments("三塁へは盗塁を試行しない", Destination.THIRD, StealResult.NOT_TRY));
    }

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

    @DisplayName("設定された盗塁成功率が高いほど同じ乱数でも盗塁に成功する")
    @org.junit.jupiter.api.Test
    void usesConfiguredStealSuccessRate() {
        // given
        var lowSuccessRateStrategy =
                new EagerStealStrategy(SimulationRulesTestData.standard().eagerSteal(), 0.5f);
        var highSuccessRateStrategy =
                new EagerStealStrategy(SimulationRulesTestData.standard().eagerSteal(), 0.8f);
        try (MockedStatic<RandomGenerator> randomGenerator = mockStatic(RandomGenerator.class)) {
            randomGenerator.when(RandomGenerator::nextFloat).thenReturn(0.9f);

            // when
            StealResult lowRateResult = lowSuccessRateStrategy.runToDouble();
            StealResult highRateResult = highSuccessRateStrategy.runToDouble();

            // then
            assertAll(
                    () -> assertEquals(StealResult.FAILURE, lowRateResult),
                    () -> assertEquals(StealResult.SUCCESS, highRateResult));
        }
    }

    @DisplayName("盗塁しない戦略は進塁先にかかわらず試行しない")
    @ParameterizedTest(name = "{0}")
    @MethodSource("nowayStealTestCases")
    void neverAttemptsSteal(
            String description, Destination destination, StealResult expectedResult) {
        // given
        var strategy = new NowayStealStrategy();

        // when
        StealResult result = destination.run(strategy);

        // then
        assertAll(() -> assertEquals(expectedResult, result, description));
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

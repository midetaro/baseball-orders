package com.example.baseballorders.simulator.domain.player.strategy.batting;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

import com.example.baseballorders.simulator.domain.play.BattingResult;
import com.example.baseballorders.simulator.domain.player.strategy.RandomGenerator;
import com.example.baseballorders.simulator.domain.rule.SimulationRulesTestData;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

class ExpandedBattingResultTest {

    static Stream<Arguments> expandedResultBoundaries() {
        return Stream.of(
                Arguments.of("5%未満なら四球になる", 0.4f, 0.049999f, BattingResult.WALK),
                Arguments.of("5%と等しければ安打になる", 0.4f, 0.05f, BattingResult.HIT_SINGLE),
                Arguments.of("打率と等しい乱数でも安打になる", 0.4f, 0.4f, BattingResult.HIT_HOMER),
                Arguments.of("四球と安打の合計と等しければ三振になる", 0.4f, 0.43f, BattingResult.STRIKEOUT),
                Arguments.of(
                        "非出塁の25%未満なら三振になる", 0.4f, Math.nextDown(0.5725f), BattingResult.STRIKEOUT),
                Arguments.of("非出塁の25%と等しければ凡退になる", 0.4f, 0.5725f, BattingResult.BATTED_OUT),
                Arguments.of("低打率でも四球確率は5%のまま", 0.03f, 0.04f, BattingResult.WALK),
                Arguments.of("低打率でも四球の後に安打区間がある", 0.03f, 0.06f, BattingResult.HIT_SINGLE));
    }

    @DisplayName("四球・三振・凡退の確率境界を一つの乱数で判定する")
    @ParameterizedTest(name = "{0}")
    @MethodSource("expandedResultBoundaries")
    void determinesExpandedResultWithOneRandomDraw(
            String description, float battingAverage, float random, BattingResult expectedResult) {
        // given
        var sut =
                new MiddleDistanceHittingStrategy(
                        SimulationRulesTestData.standard().batting(),
                        SimulationRulesTestData.standard().middleDistanceHitting());
        try (MockedStatic<RandomGenerator> randomGenerator = mockStatic(RandomGenerator.class)) {
            randomGenerator.when(RandomGenerator::nextFloat).thenReturn(random);

            // when
            BattingResult result = sut.batting(battingAverage);

            // then
            assertAll(
                    () -> assertEquals(expectedResult, result, description),
                    () -> randomGenerator.verify(RandomGenerator::nextFloat, times(1)));
        }
    }
}

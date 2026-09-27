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
                Arguments.of("出塁率と等しければ三振になる", 0.4f, 0.4f, BattingResult.STRIKEOUT),
                Arguments.of("非出塁の25%未満なら三振になる", 0.4f, 0.549999f, BattingResult.STRIKEOUT),
                Arguments.of("非出塁の25%と等しければ凡退になる", 0.4f, 0.55f, BattingResult.BATTED_OUT),
                Arguments.of("出塁率が5%未満なら全出塁を四球にする", 0.03f, 0.029999f, BattingResult.WALK),
                Arguments.of("低出塁率と等しければ三振になる", 0.03f, 0.03f, BattingResult.STRIKEOUT));
    }

    @DisplayName("四球・三振・凡退の確率境界を一つの乱数で判定する")
    @ParameterizedTest(name = "{0}")
    @MethodSource("expandedResultBoundaries")
    void determinesExpandedResultWithOneRandomDraw(
            String description,
            float onBasePercentage,
            float random,
            BattingResult expectedResult) {
        // given
        var strategy =
                new MiddleDistanceHittingStrategy(
                        SimulationRulesTestData.standard().batting(),
                        SimulationRulesTestData.standard().middleDistanceHitting());
        try (MockedStatic<RandomGenerator> randomGenerator = mockStatic(RandomGenerator.class)) {
            randomGenerator.when(RandomGenerator::nextFloat).thenReturn(random);

            // when
            BattingResult result = strategy.batting(onBasePercentage, 0.55f);

            // then
            assertAll(
                    () -> assertEquals(expectedResult, result, description),
                    () -> randomGenerator.verify(RandomGenerator::nextFloat, times(1)));
        }
    }
}

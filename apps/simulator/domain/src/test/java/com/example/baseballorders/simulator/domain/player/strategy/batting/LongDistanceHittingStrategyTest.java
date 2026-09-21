package com.example.baseballorders.simulator.domain.player.strategy.batting;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mockStatic;

import com.example.baseballorders.simulator.domain.play.BattingResult;
import com.example.baseballorders.simulator.domain.player.strategy.RandomGenerator;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

class LongDistanceHittingStrategyTest {

    @DisplayName("乱数と打撃成績に応じて長距離バッターの打席結果を決定する")
    @ParameterizedTest(name = "{0}")
    @MethodSource("battingTestCases")
    void determinesBattingResult(String description, float random, BattingResult expectedResult) {
        // given
        var behavior = new LongDistanceHittingStrategy();
        try (MockedStatic<RandomGenerator> randomGenerator = mockStatic(RandomGenerator.class)) {
            randomGenerator.when(RandomGenerator::nextFloat).thenReturn(random);

            // when
            BattingResult result = behavior.batting(0.4f, 0.55f);

            // then
            assertAll(() -> assertEquals(expectedResult, result, description));
        }
    }

    static Stream<Arguments> battingTestCases() {
        return Stream.of(
                arguments("5%未満なら四球になる", 0.04f, BattingResult.WALK),
                arguments("長距離打者の単打配分なら単打になる", 0.29f, BattingResult.HIT_SINGLE),
                arguments("長距離打者の二塁打配分なら二塁打になる", 0.30f, BattingResult.HIT_DOUBLE),
                arguments("長距離打者の三塁打配分なら三塁打になる", 0.32f, BattingResult.HIT_TRIPLE),
                arguments("長距離打者の本塁打配分なら本塁打になる", 0.35f, BattingResult.HIT_HOMER),
                arguments("出塁率と等しければ三振になる", 0.4f, BattingResult.STRIKEOUT),
                arguments("非出塁の25%以降なら凡退になる", 0.55f, BattingResult.BATTED_OUT));
    }
}

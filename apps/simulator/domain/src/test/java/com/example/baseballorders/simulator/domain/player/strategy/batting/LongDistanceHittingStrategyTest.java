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
                arguments("単打確率未満なら単打になる", 0.24f, BattingResult.HIT_SINGLE),
                arguments("単打確率と等しければ二塁打になる", 0.25f, BattingResult.HIT_DOUBLE),
                arguments("二塁打確率と等しければ三塁打になる", 0.26875f, BattingResult.HIT_TRIPLE),
                arguments("三塁打確率を超えれば本塁打になる", 0.29f, BattingResult.HIT_HOMER),
                arguments("長距離バッターの安打確率と等しければアウトになる", 0.3625f, BattingResult.OUT));
    }
}

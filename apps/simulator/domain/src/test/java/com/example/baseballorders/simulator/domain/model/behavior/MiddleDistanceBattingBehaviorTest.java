package com.example.baseballorders.simulator.domain.model.behavior;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mockStatic;

import com.example.baseballorders.simulator.domain.code.BattingResult;
import com.example.baseballorders.simulator.domain.util.RandomGenerator;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

class MiddleDistanceBattingBehaviorTest {

    @DisplayName("乱数と打撃成績に応じて打席結果を決定する")
    @ParameterizedTest(name = "{0}")
    @MethodSource("battingTestCases")
    void determinesBattingResult(String description, float random, BattingResult expectedResult) {
        // given
        var behavior = new MiddleDistanceBattingBehavior();
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
                arguments("単打確率未満なら単打になる", 0.32f, BattingResult.HIT_SINGLE),
                arguments("単打確率と等しければ二塁打になる", 0.325f, BattingResult.HIT_DOUBLE),
                arguments("二塁打確率と等しければ三塁打になる", 0.35f, BattingResult.HIT_TRIPLE),
                arguments("三塁打確率と等しければ本塁打になる", 0.375f, BattingResult.HIT_HOMER),
                arguments("安打確率と等しければアウトになる", 0.4f, BattingResult.OUT));
    }
}

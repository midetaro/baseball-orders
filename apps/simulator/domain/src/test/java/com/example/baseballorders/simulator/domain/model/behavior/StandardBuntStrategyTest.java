package com.example.baseballorders.simulator.domain.model.behavior;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mockStatic;

import com.example.baseballorders.simulator.domain.code.BuntResult;
import com.example.baseballorders.simulator.domain.util.RandomGenerator;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

class StandardBuntStrategyTest {

    @DisplayName("乱数がバント成功率未満の場合だけバントに成功する")
    @ParameterizedTest(name = "{0}")
    @MethodSource("buntTestCases")
    void determinesBuntResult(String description, float random, BuntResult expectedResult) {
        // given
        var strategy = new StandardBuntStrategy();
        try (MockedStatic<RandomGenerator> randomGenerator = mockStatic(RandomGenerator.class)) {
            // バント判定に使う乱数を固定する
            randomGenerator.when(RandomGenerator::nextFloat).thenReturn(random);

            // when
            BuntResult result = strategy.bunt(0.7f);

            // then
            assertAll(() -> assertEquals(expectedResult, result, description));
        }
    }

    static Stream<Arguments> buntTestCases() {
        return Stream.of(
                arguments("成功率未満なら成功する", 0.69f, BuntResult.SUCCESS),
                arguments("成功率と等しければ失敗する", 0.7f, BuntResult.FAILURE));
    }
}

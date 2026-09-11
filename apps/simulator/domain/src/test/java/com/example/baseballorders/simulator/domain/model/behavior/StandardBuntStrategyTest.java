package com.example.baseballorders.simulator.domain.model.behavior;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mockStatic;

import com.example.baseballorders.simulator.domain.code.BuntResult;
import com.example.baseballorders.simulator.domain.code.OutCount;
import com.example.baseballorders.simulator.domain.model.state.BasesState;
import com.example.baseballorders.simulator.domain.model.state.DoubleBaseState;
import com.example.baseballorders.simulator.domain.model.state.FirstDoubleBaseState;
import com.example.baseballorders.simulator.domain.model.state.NoBasesState;
import com.example.baseballorders.simulator.domain.model.state.SingleBasesState;
import com.example.baseballorders.simulator.domain.util.RandomGenerator;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

class StandardBuntStrategyTest {

    @DisplayName("標準戦略は無死一塁と無死一二塁だけバントする")
    @ParameterizedTest(name = "{0}")
    @MethodSource("buntTestCases")
    void determinesBuntResult(
            String description,
            OutCount outCount,
            BasesState basesState,
            float random,
            BuntResult expectedResult) {
        // given
        var strategy = new StandardBuntStrategy();
        try (MockedStatic<RandomGenerator> randomGenerator = mockStatic(RandomGenerator.class)) {
            // バント判定に使う乱数を固定する
            randomGenerator.when(RandomGenerator::nextFloat).thenReturn(random);

            // when
            BuntResult result = strategy.bunt(0.7f, outCount, basesState);

            // then
            assertAll(() -> assertEquals(expectedResult, result, description));
        }
    }

    static Stream<Arguments> buntTestCases() {
        return Stream.of(
                arguments(
                        "無死一塁で成功率未満なら成功する",
                        OutCount.NO_OUT,
                        new SingleBasesState(),
                        0.69f,
                        BuntResult.SUCCESS),
                arguments(
                        "無死一塁で成功率と等しければ失敗する",
                        OutCount.NO_OUT,
                        new SingleBasesState(),
                        0.7f,
                        BuntResult.FAILURE),
                arguments(
                        "無死一二塁ならバントする",
                        OutCount.NO_OUT,
                        new FirstDoubleBaseState(),
                        0.1f,
                        BuntResult.SUCCESS),
                arguments(
                        "一死一塁ならバントしない",
                        OutCount.ONE_OUT,
                        new SingleBasesState(),
                        0.1f,
                        BuntResult.NOT_TRY),
                arguments(
                        "無死二塁ならバントしない",
                        OutCount.NO_OUT,
                        new DoubleBaseState(),
                        0.1f,
                        BuntResult.NOT_TRY),
                arguments(
                        "無死走者なしならバントしない",
                        OutCount.NO_OUT,
                        new NoBasesState(),
                        0.1f,
                        BuntResult.NOT_TRY));
    }
}

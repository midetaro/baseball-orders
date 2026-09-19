package com.example.baseballorders.simulator.domain.model.behavior.bunt;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mockStatic;

import com.example.baseballorders.simulator.domain.code.BuntResult;
import com.example.baseballorders.simulator.domain.code.OutCount;
import com.example.baseballorders.simulator.domain.entity.player.BatterEntity;
import com.example.baseballorders.simulator.domain.model.BatterTestDataFactory;
import com.example.baseballorders.simulator.domain.model.state.BasesState;
import com.example.baseballorders.simulator.domain.model.state.base.DoubleBaseState;
import com.example.baseballorders.simulator.domain.model.state.base.FirstDoubleBaseState;
import com.example.baseballorders.simulator.domain.model.state.base.NoBasesState;
import com.example.baseballorders.simulator.domain.model.state.base.SingleBasesState;
import com.example.baseballorders.simulator.domain.util.RandomGenerator;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

class EagerBuntStrategyTest {
    private static final BatterEntity RUNNER = BatterTestDataFactory.mock().getFirst();

    @DisplayName("積極的戦略は試合状況によらず指定された成功率でバント結果を判定する")
    @ParameterizedTest(name = "{0}")
    @MethodSource("buntTestCases")
    void determinesBuntResultBySuccessRate(
            String description,
            OutCount outCount,
            BasesState basesState,
            float random,
            BuntResult expectedResult) {
        // given
        var strategy = new EagerBuntStrategy();
        try (MockedStatic<RandomGenerator> randomGenerator = mockStatic(RandomGenerator.class)) {
            // バントの成否判定に使う乱数を固定する
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
                        "無死一塁ならバントする",
                        OutCount.NO_OUT,
                        new SingleBasesState(RUNNER),
                        0.1f,
                        BuntResult.SUCCESS),
                arguments(
                        "無死一二塁ならバントする",
                        OutCount.NO_OUT,
                        new FirstDoubleBaseState(RUNNER, RUNNER),
                        0.1f,
                        BuntResult.SUCCESS),
                arguments(
                        "無死二塁ならバントする",
                        OutCount.NO_OUT,
                        new DoubleBaseState(RUNNER),
                        0.1f,
                        BuntResult.SUCCESS),
                arguments(
                        "一死一塁ならバントする",
                        OutCount.ONE_OUT,
                        new SingleBasesState(RUNNER),
                        0.1f,
                        BuntResult.SUCCESS),
                arguments(
                        "成功率と等しければ失敗する",
                        OutCount.NO_OUT,
                        new SingleBasesState(RUNNER),
                        0.7f,
                        BuntResult.FAILURE),
                arguments(
                        "無死走者なしでも成功率に従って判定する",
                        OutCount.NO_OUT,
                        new NoBasesState(),
                        0.1f,
                        BuntResult.SUCCESS),
                arguments(
                        "一死二塁でも成功率に従って判定する",
                        OutCount.ONE_OUT,
                        new DoubleBaseState(RUNNER),
                        0.1f,
                        BuntResult.SUCCESS),
                arguments(
                        "二死のときバントを実行しない",
                        OutCount.TWO_OUT,
                        new SingleBasesState(RUNNER),
                        0.1f,
                        BuntResult.NOT_TRY));
    }
}

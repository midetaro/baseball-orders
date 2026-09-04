package com.example.baseballorders.simulator.domain.model.behavior;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mockStatic;

import com.example.baseballorders.simulator.domain.code.BuntResult;
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

class EagerBuntStrategyTest {

    @DisplayName("積極的戦略は指定された走者とアウトの状況だけバントする")
    @ParameterizedTest(name = "{0}")
    @MethodSource("buntTestCases")
    void buntsOnlyInEagerSituations(
            String description,
            long outCounts,
            BasesState basesState,
            float random,
            BuntResult expectedResult) {
        // given
        var strategy = new EagerBuntStrategy();
        try (MockedStatic<RandomGenerator> randomGenerator = mockStatic(RandomGenerator.class)) {
            // バントの成否判定に使う乱数を固定する
            randomGenerator.when(RandomGenerator::nextFloat).thenReturn(random);

            // when
            BuntResult result = strategy.bunt(0.7f, outCounts, basesState);

            // then
            assertAll(() -> assertEquals(expectedResult, result, description));
        }
    }

    static Stream<Arguments> buntTestCases() {
        return Stream.of(
                arguments("無死一塁ならバントする", 0, new SingleBasesState(), 0.1f, BuntResult.SUCCESS),
                arguments("無死一二塁ならバントする", 0, new FirstDoubleBaseState(), 0.1f, BuntResult.SUCCESS),
                arguments("無死二塁ならバントする", 0, new DoubleBaseState(), 0.1f, BuntResult.SUCCESS),
                arguments("一死一塁ならバントする", 1, new SingleBasesState(), 0.1f, BuntResult.SUCCESS),
                arguments("成功率と等しければ失敗する", 0, new SingleBasesState(), 0.7f, BuntResult.FAILURE),
                arguments("無死走者なしならバントしない", 0, new NoBasesState(), 0.1f, BuntResult.NOT_TRY),
                arguments("一死二塁ならバントしない", 1, new DoubleBaseState(), 0.1f, BuntResult.NOT_TRY),
                arguments("二死一塁ならバントしない", 2, new SingleBasesState(), 0.1f, BuntResult.NOT_TRY));
    }
}

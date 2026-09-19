package com.example.baseballorders.simulator.domain.model.state.base;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.example.baseballorders.simulator.domain.entity.player.BatterEntity;
import com.example.baseballorders.simulator.domain.model.BatterTestDataFactory;
import com.example.baseballorders.simulator.domain.model.state.BasesState;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class BaseStateFactoryTest {

    private static final BatterEntity RUNNER = BatterTestDataFactory.mock().getFirst();

    @DisplayName("走者配置に対応した塁状態を生成する")
    @ParameterizedTest(name = "{0}")
    @MethodSource("baseStates")
    void createsBaseStateForRunnerConfiguration(
            String description,
            BatterEntity first,
            BatterEntity second,
            BatterEntity third,
            Class<? extends BasesState> expected) {
        // given
        BaseStateFactory factory = new BaseStateFactory();

        // when
        BasesState actual = factory.create(first, second, third);

        // then
        assertAll(() -> assertInstanceOf(expected, actual, description));
    }

    static Stream<Arguments> baseStates() {
        return Stream.of(
                arguments("走者なし", null, null, null, NoBasesState.class),
                arguments("一塁走者", RUNNER, null, null, SingleBasesState.class),
                arguments("二塁走者", null, RUNNER, null, DoubleBaseState.class),
                arguments("三塁走者", null, null, RUNNER, ThirdBaseState.class),
                arguments("一二塁走者", RUNNER, RUNNER, null, FirstDoubleBaseState.class),
                arguments("一三塁走者", RUNNER, null, RUNNER, FirstThirdBaseState.class),
                arguments("二三塁走者", null, RUNNER, RUNNER, DoubleThirdBaseState.class),
                arguments("満塁", RUNNER, RUNNER, RUNNER, FullBasesState.class));
    }
}

package com.example.baseballorders.simulator.domain.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.example.baseballorders.simulator.domain.model.player.BatterEntity;
import com.example.baseballorders.simulator.domain.model.state.BasesState;
import com.example.baseballorders.simulator.domain.model.state.DoubleBaseState;
import com.example.baseballorders.simulator.domain.model.state.DoubleThirdBaseState;
import com.example.baseballorders.simulator.domain.model.state.FirstDoubleBaseState;
import com.example.baseballorders.simulator.domain.model.state.FirstThirdBaseState;
import com.example.baseballorders.simulator.domain.model.state.FullBasesState;
import com.example.baseballorders.simulator.domain.model.state.NoBasesState;
import com.example.baseballorders.simulator.domain.model.state.SingleBasesState;
import com.example.baseballorders.simulator.domain.model.state.ThirdBaseState;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class BasesStateResolverTest {

    @DisplayName("走者配置に対応する塁状態を返す")
    @ParameterizedTest(name = "{0}")
    @MethodSource("resolveTestCases")
    void resolvesState(
            String description,
            boolean hasFirst,
            boolean hasSecond,
            boolean hasThird,
            Class<? extends BasesState> expectedState) {
        // given
        BatterEntity runner = BatterTestDataFactory.mock().getFirst();
        BaseRunners runners =
                new BaseRunners(
                        hasFirst ? runner : null,
                        hasSecond ? runner : null,
                        hasThird ? runner : null);

        // when
        BasesState state = BasesStateResolver.resolve(runners);

        // then
        assertAll(description, () -> assertInstanceOf(expectedState, state));
    }

    static Stream<Arguments> resolveTestCases() {
        return Stream.of(
                arguments("満塁", true, true, true, FullBasesState.class),
                arguments("一・二塁", true, true, false, FirstDoubleBaseState.class),
                arguments("一・三塁", true, false, true, FirstThirdBaseState.class),
                arguments("一塁", true, false, false, SingleBasesState.class),
                arguments("二・三塁", false, true, true, DoubleThirdBaseState.class),
                arguments("二塁", false, true, false, DoubleBaseState.class),
                arguments("三塁", false, false, true, ThirdBaseState.class),
                arguments("走者なし", false, false, false, NoBasesState.class));
    }
}

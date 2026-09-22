package com.example.baseballorders.simulator.domain.game;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.example.baseballorders.simulator.domain.play.OutCount;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class BaseStateFactoryTest {
    static Stream<Arguments> configurations() {
        return Stream.of(
                arguments(0, NoBasesState.class),
                arguments(1, SingleBasesState.class),
                arguments(2, DoubleBaseState.class),
                arguments(3, FirstDoubleBaseState.class),
                arguments(4, ThirdBaseState.class),
                arguments(5, FirstThirdBaseState.class),
                arguments(6, DoubleThirdBaseState.class),
                arguments(7, FullBasesState.class));
    }

    @Test
    @DisplayName("ファクトリから作るStateは試合間で共有しない")
    void createsIndependentGameStates() {
        // given
        var first = GameStateTestFixture.context(null, null, null, OutCount.NO_OUT);
        var second = GameStateTestFixture.context(null, null, null, OutCount.NO_OUT);
        // when
        first.inningStateContext().currentBaseState().out();
        // then
        assertAll(
                () ->
                        assertNotSame(
                                first.inningStateContext().currentBaseState(),
                                second.inningStateContext().currentBaseState()),
                () ->
                        assertEquals(
                                OutCount.ONE_OUT,
                                first.inningStateContext().currentBaseState().getOutCount()),
                () ->
                        assertEquals(
                                OutCount.NO_OUT,
                                second.inningStateContext().currentBaseState().getOutCount()));
    }

    @ParameterizedTest(name = "配置{0}")
    @MethodSource("configurations")
    @DisplayName("走者配置から対応するStateを生成する")
    void createsStateForConfiguration(int configuration, Class<? extends BasesState> stateType) {
        // given
        var sut = new BaseStateFactory();
        var context = GameStateTestFixture.context(null, null, null, OutCount.NO_OUT);

        // when
        var state = sut.create(context.inningStateContext(), configuration);

        // then
        assertAll(() -> assertEquals(stateType, state.getClass()));
    }

    @Test
    @DisplayName("不正な走者配置では例外にする")
    void rejectsInvalidConfiguration() {
        // given
        var sut = new BaseStateFactory();
        var context = GameStateTestFixture.context(null, null, null, OutCount.NO_OUT);

        // when
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> sut.create(context.inningStateContext(), 8));

        // then
        assertAll(() -> assertEquals("不正な走者配置: 8", exception.getMessage()));
    }
}

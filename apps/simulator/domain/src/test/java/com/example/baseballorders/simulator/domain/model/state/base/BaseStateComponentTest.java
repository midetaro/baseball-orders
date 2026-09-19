package com.example.baseballorders.simulator.domain.model.state.base;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.baseballorders.simulator.domain.model.state.BasesState;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.stereotype.Component;

class BaseStateComponentTest {

    @DisplayName("塁状態はBean化せず状態ファクトリだけをSpringコンポーネントにする")
    @Test
    void onlyBaseStateFactoryIsComponent() throws ClassNotFoundException {
        // given
        List<Class<? extends BasesState>> baseStates =
                List.of(
                        DoubleBaseState.class,
                        DoubleThirdBaseState.class,
                        FirstDoubleBaseState.class,
                        FirstThirdBaseState.class,
                        FullBasesState.class,
                        NoBasesState.class,
                        SingleBasesState.class,
                        ThirdBaseState.class);

        // when
        Class<?> factory =
                Class.forName(
                        "com.example.baseballorders.simulator.domain.model.state.base.BaseStateFactory");

        // then
        assertAll(
                Stream.concat(
                                Stream.of(
                                        (Executable)
                                                () ->
                                                        assertTrue(
                                                                factory.isAnnotationPresent(
                                                                        Component.class))),
                                baseStates.stream()
                                        .map(
                                                baseState ->
                                                        (Executable)
                                                                () ->
                                                                        assertFalse(
                                                                                baseState
                                                                                        .isAnnotationPresent(
                                                                                                Component
                                                                                                        .class),
                                                                                baseState
                                                                                        ::getSimpleName)))
                        .toList());
    }
}

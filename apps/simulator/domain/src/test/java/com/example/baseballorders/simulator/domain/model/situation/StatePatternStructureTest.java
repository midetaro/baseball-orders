package com.example.baseballorders.simulator.domain.model.situation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.baseballorders.simulator.domain.code.BuntResult;
import com.example.baseballorders.simulator.domain.code.StealResult;
import com.example.baseballorders.simulator.domain.entity.player.BatterEntity;
import com.example.baseballorders.simulator.domain.model.GameBattingContext;
import com.example.baseballorders.simulator.domain.model.base.*;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class StatePatternStructureTest {

    private static final List<Class<? extends BasesState>> STATE_TYPES =
            List.of(
                    NoBasesState.class,
                    SingleBasesState.class,
                    DoubleBaseState.class,
                    FirstDoubleBaseState.class,
                    ThirdBaseState.class,
                    FirstThirdBaseState.class,
                    DoubleThirdBaseState.class,
                    FullBasesState.class);

    @Test
    @DisplayName("BasesStateはinterfaceで全Stateが7種類のプレーメソッドをoverrideする")
    void overridesPlayMethodsDeclaredByBasesState() throws ReflectiveOperationException {
        // given
        var methods =
                List.of(
                        expectedMethod("bunt", BuntResult.class, BatterEntity.class),
                        expectedMethod("hitSingle", void.class, BatterEntity.class),
                        expectedMethod("hitDouble", void.class, BatterEntity.class),
                        expectedMethod("hitTriple", void.class, BatterEntity.class),
                        expectedMethod("hitHomer", void.class),
                        expectedMethod("stealToDouble", StealResult.class),
                        expectedMethod("stealToTriple", StealResult.class));

        // when
        List<Executable> inheritanceChecks =
                STATE_TYPES.stream()
                        .flatMap(
                                stateType ->
                                        methods.stream()
                                                .map(
                                                        expected ->
                                                                (Executable)
                                                                        () -> {
                                                                            var overridden =
                                                                                    stateType
                                                                                            .getMethod(
                                                                                                    expected
                                                                                                            .name(),
                                                                                                    expected
                                                                                                            .parameterTypes());
                                                                            assertEquals(
                                                                                    stateType,
                                                                                    overridden
                                                                                            .getDeclaringClass());
                                                                            assertEquals(
                                                                                    expected
                                                                                            .returnType(),
                                                                                    overridden
                                                                                            .getReturnType());
                                                                        }))
                        .toList();
        List<Executable> implementationChecks =
                STATE_TYPES.stream()
                        .map(
                                stateType ->
                                        (Executable)
                                                () ->
                                                        assertTrue(
                                                                Arrays.asList(
                                                                                stateType
                                                                                        .getInterfaces())
                                                                        .contains(
                                                                                BasesState.class)))
                        .toList();

        // then
        assertAll(
                () -> assertTrue(BasesState.class.isInterface()),
                () -> assertAll(implementationChecks),
                () -> assertAll(inheritanceChecks));
    }

    @Test
    @DisplayName("Contextと全Stateは結果Enumの各定数に対応するイベントメソッドを持つ")
    void exposesOneMethodForEachResultConstant() throws ReflectiveOperationException {
        // given
        List<Class<?>> eventReceivers =
                java.util.stream.Stream.concat(
                                java.util.stream.Stream.of(GameBattingContext.class),
                                STATE_TYPES.stream())
                        .toList();

        // when
        List<Executable> methodChecks =
                eventReceivers.stream()
                        .flatMap(
                                type ->
                                        java.util.stream.Stream.of(
                                                methodCheck(type, "out"),
                                                methodCheck(type, "hitSingle", BatterEntity.class),
                                                methodCheck(type, "hitDouble", BatterEntity.class),
                                                methodCheck(type, "hitTriple", BatterEntity.class),
                                                methodCheck(type, "hitHomer"),
                                                methodCheck(type, "buntNotTry"),
                                                methodCheck(type, "buntFailure"),
                                                methodCheck(type, "buntSuccess"),
                                                methodCheck(type, "stealNotTry"),
                                                methodCheck(type, "stealFailure"),
                                                methodCheck(type, "stealSuccess")))
                        .toList();

        // then
        assertAll(methodChecks);
    }

    @Test
    @DisplayName("Contextは試合ごとの8個のStateとcurrentStateを保持する")
    void contextOwnsPerGameStates() throws NoSuchFieldException {
        // given
        var fields = Arrays.asList(GameBattingContext.class.getDeclaredFields());

        // when
        long stateFieldCount =
                fields.stream().filter(field -> STATE_TYPES.contains(field.getType())).count();
        var currentState = GameBattingContext.class.getDeclaredField("currentState");

        // then
        assertAll(
                () -> assertEquals(8, stateFieldCount),
                () -> assertEquals(BasesState.class, currentState.getType()),
                () -> assertFalse(Modifier.isStatic(currentState.getModifiers())));
    }

    private static Executable methodCheck(
            Class<?> receiver, String methodName, Class<?>... parameterTypes) {
        return () ->
                assertEquals(
                        void.class, receiver.getMethod(methodName, parameterTypes).getReturnType());
    }

    private static ExpectedMethod expectedMethod(
            String name, Class<?> returnType, Class<?>... parameterTypes) {
        return new ExpectedMethod(name, returnType, parameterTypes);
    }

    private record ExpectedMethod(String name, Class<?> returnType, Class<?>[] parameterTypes) {}
}

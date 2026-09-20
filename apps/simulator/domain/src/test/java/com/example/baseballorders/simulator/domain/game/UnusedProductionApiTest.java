package com.example.baseballorders.simulator.domain.game;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.example.baseballorders.simulator.domain.player.BatterEntity;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class UnusedProductionApiTest {

    @DisplayName("プロダクトコードに未使用のメソッドを定義しない")
    @ParameterizedTest(name = "{0}")
    @MethodSource("unusedMethodTestCases")
    void doesNotExposeUnusedMethod(
            String description,
            Class<?> targetClass,
            String methodName,
            Class<?>[] parameterTypes) {
        // given
        // when
        NoSuchMethodException exception =
                assertThrows(
                        NoSuchMethodException.class,
                        () -> targetClass.getDeclaredMethod(methodName, parameterTypes));

        // then
        assertAll(description, () -> assertTrue(exception.getMessage().contains(methodName)));
    }

    static Stream<Arguments> unusedMethodTestCases() {
        return Stream.of(
                arguments(
                        "Contextのアウト数 getter",
                        GameBattingContext.class,
                        "getOutCount",
                        new Class<?>[0]),
                arguments(
                        "打順 getter",
                        GameBattingContext.class,
                        "getBatterEntityOrders",
                        new Class<?>[0]),
                arguments(
                        "次打者番号 getter",
                        GameBattingContext.class,
                        "getNumberOfNextBatter",
                        new Class<?>[0]),
                arguments(
                        "塁状態 setter",
                        GameBattingContext.class,
                        "updateBaseState",
                        new Class<?>[] {BasesState.class}),
                arguments(
                        "本塁打数 getter",
                        GameBattingContext.class,
                        "getHomeRunCount",
                        new Class<?>[0]),
                arguments(
                        "ソロ本塁打数 getter",
                        GameBattingContext.class,
                        "getSoloHomeRunCount",
                        new Class<?>[0]),
                arguments(
                        "2点本塁打数 getter",
                        GameBattingContext.class,
                        "getTwoRunHomeRunCount",
                        new Class<?>[0]),
                arguments(
                        "3点本塁打数 getter",
                        GameBattingContext.class,
                        "getThreeRunHomeRunCount",
                        new Class<?>[0]),
                arguments(
                        "満塁本塁打数 getter",
                        GameBattingContext.class,
                        "getGrandSlamCount",
                        new Class<?>[0]),
                arguments(
                        "成功バント数 getter", GameBattingContext.class, "getBuntCount", new Class<?>[0]),
                arguments(
                        "成功盗塁数 getter", GameBattingContext.class, "getStealCount", new Class<?>[0]),
                arguments("塁番号 getter", Base.class, "getNumber", new Class<?>[0]),
                arguments("選手名 getter", BatterEntity.class, "getName", new Class<?>[0]),
                arguments("出塁率 getter", BatterEntity.class, "getOnBasePercentage", new Class<?>[0]),
                arguments("長打率 getter", BatterEntity.class, "getSluggish", new Class<?>[0]),
                arguments(
                        "バント成功率 getter", BatterEntity.class, "getBuntSuccessRate", new Class<?>[0]),
                arguments(
                        "盗塁成功率 getter", BatterEntity.class, "getStealSuccessRate", new Class<?>[0]),
                arguments("打撃戦略 getter", BatterEntity.class, "getAtBatBehavior", new Class<?>[0]),
                arguments("盗塁戦略 getter", BatterEntity.class, "getStealStrategy", new Class<?>[0]),
                arguments("バント戦略 getter", BatterEntity.class, "getBuntStrategy", new Class<?>[0]));
    }
}

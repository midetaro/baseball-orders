package com.example.baseballorders.simulator.application;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.baseballorders.simulator.application.usecase.SimulateGameUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UnusedProductionApiTest {

    @Test
    @DisplayName("ユースケースに未使用の戦略一覧getterを定義しない")
    void doesNotExposeBehaviors() {
        // given
        Class<SimulateGameUseCase> useCaseClass = SimulateGameUseCase.class;

        // when
        NoSuchMethodException exception =
                assertThrows(
                        NoSuchMethodException.class,
                        () -> useCaseClass.getDeclaredMethod("getBehaviors"));

        // then
        assertAll(() -> assertTrue(exception.getMessage().contains("getBehaviors")));
    }
}

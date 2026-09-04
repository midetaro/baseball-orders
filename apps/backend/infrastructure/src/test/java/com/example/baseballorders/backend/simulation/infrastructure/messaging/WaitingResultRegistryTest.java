package com.example.baseballorders.backend.simulation.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.baseballorders.backend.simulation.application.WaitingResultRegistry;
import com.example.baseballorders.backend.simulation.domain.SimulationResult;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WaitingResultRegistryTest {

    @Test
    @DisplayName("待機を登録して結果を受信すると対応するFutureだけが完了して削除される")
    void completesAndRemovesMatchingWait() {
        // given
        var registry = new WaitingResultRegistry();
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        var first = registry.register(firstId);
        var second = registry.register(secondId);
        var result = new SimulationResult(firstId, 5, 4);

        // when
        boolean completed = registry.complete(firstId, result);

        // then
        assertAll(
                () -> assertTrue(completed),
                () -> assertEquals(result, first.join()),
                () -> assertFalse(second.isDone()),
                () -> assertEquals(1, registry.pendingCount()));
    }

    @Test
    @DisplayName("完了済みまたは未知のsimulation IDの重複結果は無視される")
    void ignoresDuplicateOrUnknownResult() {
        // given
        var registry = new WaitingResultRegistry();
        UUID simulationId = UUID.randomUUID();
        var result = new SimulationResult(simulationId, 5, 4);
        registry.register(simulationId);
        registry.complete(simulationId, result);

        // when
        boolean duplicateCompleted = registry.complete(simulationId, result);

        // then
        assertAll(
                () -> assertFalse(duplicateCompleted),
                () -> assertEquals(0, registry.pendingCount()));
    }

    @Test
    @DisplayName("呼出側ですでに完了したFutureへの結果は再完了せず待機を削除する")
    void removesAlreadyCompletedFuture() {
        // given
        var registry = new WaitingResultRegistry();
        UUID simulationId = UUID.randomUUID();
        var result = new SimulationResult(simulationId, 5, 4);
        registry.register(simulationId).complete(result);

        // when
        boolean completed = registry.complete(simulationId, result);

        // then
        assertAll(() -> assertFalse(completed), () -> assertEquals(0, registry.pendingCount()));
    }

    @Test
    @DisplayName("同じsimulation IDの待機を二重登録すると拒否される")
    void rejectsDuplicateRegistration() {
        // given
        var registry = new WaitingResultRegistry();
        UUID simulationId = UUID.randomUUID();
        registry.register(simulationId);

        // when
        var exception =
                assertThrows(IllegalStateException.class, () -> registry.register(simulationId));

        // then
        assertAll(
                () ->
                        assertEquals(
                                "simulation already registered: " + simulationId,
                                exception.getMessage()));
    }
}

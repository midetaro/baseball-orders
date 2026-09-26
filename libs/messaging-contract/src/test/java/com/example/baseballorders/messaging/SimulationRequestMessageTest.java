package com.example.baseballorders.messaging;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SimulationRequestMessageTest {

    @Test
    @DisplayName("投手性格をSQS要求メッセージに含める")
    void includesPitcherPersonality() {
        // given
        var simulationId = UUID.randomUUID();

        // when
        var request =
                new SimulationRequestMessage(
                        simulationId,
                        "1",
                        java.util.List.of(),
                        PitcherPersonality.BOLD,
                        SimulationMode.SINGLE_GAME_RUN);

        // then
        assertAll(
                () -> assertEquals(simulationId, request.simulationId()),
                () -> assertEquals(PitcherPersonality.BOLD, request.pitcherPersonality()),
                () -> assertEquals(SimulationMode.SINGLE_GAME_RUN, request.mode()));
    }

    @Test
    @DisplayName("互換コンストラクタは大規模実行モードを既定値にする")
    void defaultsModeToLargeScaleRunForLegacyCallers() {
        // given
        var simulationId = UUID.randomUUID();

        // when
        var request = new SimulationRequestMessage(simulationId, "1", java.util.List.of());

        // then
        assertAll(() -> assertEquals(SimulationMode.LARGE_SCALE_RUN, request.mode()));
    }

    @Test
    @DisplayName("正準コンストラクタにnullのモードを渡すと大規模実行モードを既定値にする")
    void defaultsNullModeToLargeScaleRun() {
        // given
        var simulationId = UUID.randomUUID();

        // when
        var request =
                new SimulationRequestMessage(
                        simulationId, "1", java.util.List.of(), PitcherPersonality.BOLD, null);

        // then
        assertAll(() -> assertEquals(SimulationMode.LARGE_SCALE_RUN, request.mode()));
    }
}

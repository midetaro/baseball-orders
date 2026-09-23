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
                        simulationId, "1", java.util.List.of(), PitcherPersonality.BOLD);

        // then
        assertAll(
                () -> assertEquals(simulationId, request.simulationId()),
                () -> assertEquals(PitcherPersonality.BOLD, request.pitcherPersonality()));
    }
}

package com.example.baseballorders.backend.simulation.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.example.baseballorders.backend.application.WaitingResultRegistry;
import com.example.baseballorders.messaging.SimulationResultMessage;
import io.awspring.cloud.sqs.annotation.SqsListener;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SimulationResultListenerTest {

    @Test
    @DisplayName("simulation-resultを受信すると同じsimulation IDのHTTP待機を完了する")
    void correlatesReceivedResult() {
        // given
        var registry = new WaitingResultRegistry();
        UUID simulationId = UUID.randomUUID();
        var waiting = registry.register(simulationId);
        var listener = new SimulationResultListener(registry);

        // when
        listener.receive(new SimulationResultMessage(simulationId, "1", 5, 4));

        // then
        assertAll(
                () -> assertEquals(simulationId, waiting.join().simulationId()),
                () -> assertEquals(5, waiting.join().score()),
                () -> assertEquals(4, waiting.join().runs()));
    }

    @Test
    @DisplayName("Listenerはsimulation-resultキューを購読する")
    void listensToSimulationResultQueue() throws NoSuchMethodException {
        // given
        var method =
                SimulationResultListener.class.getMethod("receive", SimulationResultMessage.class);

        // when
        var annotation = method.getAnnotation(SqsListener.class);

        // then
        assertAll(
                () ->
                        assertEquals(
                                List.of("${simulation.sqs.result-queue-name}"),
                                List.of(annotation.value())));
    }

    @Test
    @DisplayName("待機がない遅延結果を受信しても新しい待機は作成されない")
    void ignoresLateResult() {
        // given
        var registry = new WaitingResultRegistry();
        var listener = new SimulationResultListener(registry);

        // when
        listener.receive(new SimulationResultMessage(UUID.randomUUID(), "1", 5, 4));

        // then
        assertAll(() -> assertFalse(registry.pendingCount() > 0));
    }
}

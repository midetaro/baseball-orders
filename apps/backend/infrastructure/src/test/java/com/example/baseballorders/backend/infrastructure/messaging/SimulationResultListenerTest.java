package com.example.baseballorders.backend.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.*;

import com.example.baseballorders.backend.application.WaitingResultRegistry;
import com.example.baseballorders.messaging.SimulationResultMessage;
import io.awspring.cloud.sqs.annotation.SqsListener;
import java.util.List;
import java.util.Map;
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
        var sut = new SimulationResultListener(registry);

        // when
        sut.receive(
                new SimulationResultMessage(
                        simulationId,
                        "1",
                        new SimulationResultMessage.GameScoreStatistics(5, 5, 5, 10, Map.of(5, 10)),
                        new SimulationResultMessage.GameContentStatistics(
                                4, 1, 1, 1, 1, 2, 3, 5, 7, 11, 13, 17, 19, 23, 29)));

        // then
        assertAll(
                () -> assertEquals(simulationId, waiting.join().simulationId()),
                () -> assertEquals(10, waiting.join().statistics().gameCount()),
                () -> assertEquals(Map.of(5, 10), waiting.join().statistics().scoreDistribution()),
                () -> assertEquals(5, waiting.join().statistics().averageScore()),
                () -> assertEquals(5, waiting.join().statistics().medianScore()),
                () -> assertEquals(5, waiting.join().statistics().maximumScore()),
                () -> assertEquals(4, waiting.join().statistics().homeRunCount()),
                () -> assertEquals(1, waiting.join().statistics().soloHomeRunCount()),
                () -> assertEquals(1, waiting.join().statistics().twoRunHomeRunCount()),
                () -> assertEquals(1, waiting.join().statistics().threeRunHomeRunCount()),
                () -> assertEquals(1, waiting.join().statistics().grandSlamCount()),
                () -> assertEquals(2, waiting.join().statistics().buntCount()),
                () -> assertEquals(3, waiting.join().statistics().stealCount()),
                () -> assertEquals(5, waiting.join().statistics().buntFailureCount()),
                () -> assertEquals(7, waiting.join().statistics().stealFailureCount()),
                () -> assertEquals(11, waiting.join().statistics().advancingBuntCount()),
                () -> assertEquals(13, waiting.join().statistics().squeezeBuntCount()),
                () -> assertEquals(17, waiting.join().statistics().advancingBuntFailureCount()),
                () -> assertEquals(19, waiting.join().statistics().squeezeBuntFailureCount()),
                () -> assertEquals(23, waiting.join().statistics().stealToSecondCount()),
                () -> assertEquals(29, waiting.join().statistics().stealToThirdCount()));
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
        var sut = new SimulationResultListener(registry);

        // when
        sut.receive(
                new SimulationResultMessage(
                        UUID.randomUUID(),
                        "1",
                        new SimulationResultMessage.GameScoreStatistics(5, 5, 5, 1, Map.of(5, 1)),
                        new SimulationResultMessage.GameContentStatistics(
                                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)));

        // then
        assertAll(() -> assertFalse(registry.pendingCount() > 0));
    }

    @Test
    @DisplayName("統計情報がないsimulation-resultは待機結果として受理しない")
    void rejectsResultWithoutStatistics() {
        // given
        var registry = new WaitingResultRegistry();
        UUID simulationId = UUID.randomUUID();
        var waiting = registry.register(simulationId);
        var sut = new SimulationResultListener(registry);

        // when
        var exception =
                assertThrows(
                        NullPointerException.class,
                        () ->
                                sut.receive(
                                        new SimulationResultMessage(
                                                simulationId,
                                                "1",
                                                null,
                                                new SimulationResultMessage.GameContentStatistics(
                                                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                                                        0))));

        // then
        assertAll(
                () ->
                        assertEquals(
                                "simulation result statistics must not be null",
                                exception.getMessage()),
                () -> assertFalse(waiting.isDone()));
    }

    @Test
    @DisplayName("プレー内容統計がないsimulation-resultは待機結果として受理しない")
    void rejectsResultWithoutGameContentStatistics() {
        // given
        var registry = new WaitingResultRegistry();
        UUID simulationId = UUID.randomUUID();
        var waiting = registry.register(simulationId);
        var sut = new SimulationResultListener(registry);

        // when
        var exception =
                assertThrows(
                        NullPointerException.class,
                        () ->
                                sut.receive(
                                        new SimulationResultMessage(
                                                simulationId,
                                                "1",
                                                new SimulationResultMessage.GameScoreStatistics(
                                                        5, 5, 5, 1, Map.of(5, 1)),
                                                null)));

        // then
        assertAll(
                () ->
                        assertEquals(
                                "simulation result statistics must not be null",
                                exception.getMessage()),
                () -> assertFalse(waiting.isDone()));
    }
}

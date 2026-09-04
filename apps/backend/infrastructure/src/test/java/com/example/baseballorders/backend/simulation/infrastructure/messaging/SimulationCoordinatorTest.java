package com.example.baseballorders.backend.simulation.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.baseballorders.backend.simulation.application.SimulationCoordinator;
import com.example.baseballorders.backend.simulation.application.SimulationRequest;
import com.example.baseballorders.backend.simulation.application.SimulationSendException;
import com.example.baseballorders.backend.simulation.application.SimulationTimeoutException;
import com.example.baseballorders.backend.simulation.application.SimulatorMessagePublisher;
import com.example.baseballorders.backend.simulation.application.WaitingResultRegistry;
import com.example.baseballorders.backend.simulation.domain.PlayerData;
import com.example.baseballorders.backend.simulation.domain.SimulationResult;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SimulationCoordinatorTest {

    @Test
    @DisplayName("DBの選手を共有contractへ変換して送信し同じsimulation IDの結果を返す")
    void sendsContractAndReturnsCorrelatedResult() {
        // given
        var registry = new WaitingResultRegistry();
        var published = new ArrayList<SimulationRequest>();
        SimulatorMessagePublisher publisher =
                request -> {
                    published.add(request);
                    registry.complete(
                            request.simulationId(),
                            new SimulationResult(request.simulationId(), 5, 4));
                };
        var coordinator =
                new SimulationCoordinator(
                        ids ->
                                ids.stream()
                                        .map(id -> new PlayerData("山田", 0.301f, 0.501f))
                                        .toList(),
                        publisher,
                        registry,
                        Duration.ofSeconds(1));

        // when
        SimulationResult result = coordinator.simulate(playerIds(9));

        // then
        assertAll(
                () -> assertEquals(result.simulationId(), published.getFirst().simulationId()),
                () -> assertEquals("1", published.getFirst().version()),
                () -> assertEquals("山田", published.getFirst().players().getFirst().name()),
                () -> assertEquals(0.301f, published.getFirst().players().getFirst().hitAverage()),
                () -> assertEquals(0.501f, published.getFirst().players().getFirst().sluggish()),
                () -> assertEquals(5, result.score()),
                () -> assertEquals(0, registry.pendingCount()));
    }

    @Test
    @DisplayName("結果をtimeoutまで受信できない場合は待機を削除する")
    void removesWaitAfterTimeout() {
        // given
        var registry = new WaitingResultRegistry();
        var coordinator =
                new SimulationCoordinator(
                        ids -> List.of(), request -> {}, registry, Duration.ofMillis(1));

        // when
        var exception =
                assertThrows(
                        SimulationTimeoutException.class, () -> coordinator.simulate(playerIds(9)));

        // then
        assertAll(
                () ->
                        assertEquals(
                                true,
                                exception
                                        .getMessage()
                                        .startsWith(
                                                "simulation result was not received within the allowed time:")),
                () -> assertEquals(0, registry.pendingCount()));
    }

    @Test
    @DisplayName("SQS送信に失敗した場合は待機を削除して同じ失敗を返す")
    void removesWaitAfterSendFailure() {
        // given
        var registry = new WaitingResultRegistry();
        var failure = new IllegalStateException("send failed");
        var coordinator =
                new SimulationCoordinator(
                        ids -> List.of(),
                        request -> {
                            throw failure;
                        },
                        registry,
                        Duration.ofSeconds(1));

        // when
        var exception =
                assertThrows(
                        SimulationSendException.class, () -> coordinator.simulate(playerIds(9)));

        // then
        assertAll(
                () -> assertEquals(failure, exception.getCause()),
                () -> assertEquals(0, registry.pendingCount()));
    }

    @Test
    @DisplayName("待機スレッドが割り込まれると割込状態を復元して待機を削除する")
    void restoresInterruptAndRemovesWait() {
        // given
        var registry = new WaitingResultRegistry();
        var failure = new AtomicReference<Throwable>();
        var coordinator =
                new SimulationCoordinator(
                        ids -> List.of(), request -> {}, registry, Duration.ofSeconds(5));
        Thread thread =
                Thread.ofPlatform()
                        .unstarted(
                                () -> {
                                    try {
                                        coordinator.simulate(playerIds(9));
                                    } catch (Throwable exception) {
                                        failure.set(exception);
                                    }
                                });
        thread.start();

        // when
        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException exception) {
            throw new AssertionError(exception);
        }

        // then
        assertAll(
                () -> assertEquals(IllegalStateException.class, failure.get().getClass()),
                () -> assertEquals(0, registry.pendingCount()));
    }

    @Test
    @DisplayName("player IDが8件の場合はDB取得やSQS送信をせず拒否する")
    void rejectsEightPlayerIds() {
        // given
        var coordinator =
                new SimulationCoordinator(
                        ids -> List.of(),
                        request -> {},
                        new WaitingResultRegistry(),
                        Duration.ofSeconds(1));

        // when
        var exception =
                assertThrows(
                        IllegalArgumentException.class, () -> coordinator.simulate(playerIds(8)));

        // then
        assertAll(
                () ->
                        assertEquals(
                                "playerIds must contain exactly 9 entries",
                                exception.getMessage()));
    }

    @Test
    @DisplayName("player IDが10件の場合はDB取得やSQS送信をせず拒否する")
    void rejectsTenPlayerIds() {
        // given
        var coordinator =
                new SimulationCoordinator(
                        ids -> List.of(),
                        request -> {},
                        new WaitingResultRegistry(),
                        Duration.ofSeconds(1));

        // when
        var exception =
                assertThrows(
                        IllegalArgumentException.class, () -> coordinator.simulate(playerIds(10)));

        // then
        assertAll(
                () ->
                        assertEquals(
                                "playerIds must contain exactly 9 entries",
                                exception.getMessage()));
    }

    private static List<Long> playerIds(int size) {
        return java.util.stream.IntStream.rangeClosed(1, size)
                .mapToObj(number -> (long) number)
                .toList();
    }
}

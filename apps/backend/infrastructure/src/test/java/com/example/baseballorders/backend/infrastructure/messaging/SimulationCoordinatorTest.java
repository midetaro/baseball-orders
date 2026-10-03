package com.example.baseballorders.backend.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.*;

import com.example.baseballorders.backend.application.SimulationCoordinator;
import com.example.baseballorders.backend.application.SimulationLimits;
import com.example.baseballorders.backend.application.SimulationLimitsBuilder;
import com.example.baseballorders.backend.application.WaitingResultRegistry;
import com.example.baseballorders.backend.application.adapter.SimulatorMessagePublisher;
import com.example.baseballorders.backend.application.dto.SimulationRequest;
import com.example.baseballorders.backend.application.exception.SimulationSendException;
import com.example.baseballorders.backend.application.exception.SimulationTimeoutException;
import com.example.baseballorders.backend.domain.PlayerData;
import com.example.baseballorders.backend.domain.PlayerDataBuilder;
import com.example.baseballorders.backend.domain.PlayerPersonality;
import com.example.baseballorders.backend.domain.SimulationMode;
import com.example.baseballorders.backend.domain.SimulationResult;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SimulationCoordinatorTest {

    /** application.ymlの既定値と同じ上限を組み立てる。 */
    private static SimulationLimits limits(Duration resultTimeout) {
        return SimulationLimitsBuilder.simulationLimits()
                .resultTimeout(resultTimeout)
                .maximumAverageHitAverage(0.350f)
                .build();
    }

    private static List<PlayerData> players(int size) {
        return java.util.stream.IntStream.rangeClosed(1, size)
                .mapToObj(
                        number ->
                                PlayerDataBuilder.playerData()
                                        .name("山田")
                                        .hitAverage(0.301f)
                                        .buntEnabled(true)
                                        .stealEnabled(true)
                                        .personality(PlayerPersonality.DEFAULT)
                                        .build())
                .toList();
    }

    @Test
    @DisplayName("画面入力の選手を共有contractへ変換して送信し同じsimulation IDの結果を返す")
    void sendsContractAndReturnsCorrelatedResult() {
        // given
        var registry = new WaitingResultRegistry();
        var published = new ArrayList<SimulationRequest>();
        SimulatorMessagePublisher publisher =
                request -> {
                    published.add(request);
                    registry.complete(
                            request.simulationId(),
                            new SimulationResult(
                                    request.simulationId(),
                                    List.of(new SimulationResult.Result(5, 4)),
                                    new SimulationResult.Statistics(5, 5, 5)));
                };
        var coordinator =
                new SimulationCoordinator(publisher, registry, limits(Duration.ofSeconds(1)));

        // when
        SimulationResult result = coordinator.simulate(players(9));

        // then
        assertAll(
                () -> assertEquals(result.simulationId(), published.getFirst().simulationId()),
                () -> assertEquals("1", published.getFirst().version()),
                () -> assertEquals("山田", published.getFirst().players().getFirst().name()),
                () -> assertEquals(0.301f, published.getFirst().players().getFirst().hitAverage()),
                () -> assertEquals(true, published.getFirst().players().getFirst().buntEnabled()),
                () -> assertEquals(true, published.getFirst().players().getFirst().stealEnabled()),
                () -> assertEquals(5, result.statistics().maximumScore()),
                () -> assertEquals(SimulationMode.LARGE_SCALE_RUN, published.getFirst().mode()),
                () -> assertEquals(0, registry.pendingCount()));
    }

    @Test
    @DisplayName("1試合実行モードを指定すると要求に1試合実行モードが設定される")
    void sendsSingleGameModeWhenRequested() {
        // given
        var registry = new WaitingResultRegistry();
        var published = new ArrayList<SimulationRequest>();
        SimulatorMessagePublisher publisher =
                request -> {
                    published.add(request);
                    registry.complete(
                            request.simulationId(),
                            new SimulationResult(
                                    request.simulationId(),
                                    List.of(new SimulationResult.Result(5, 4)),
                                    new SimulationResult.Statistics(5, 5, 5)));
                };
        var coordinator =
                new SimulationCoordinator(publisher, registry, limits(Duration.ofSeconds(1)));

        // when
        coordinator.simulate(players(9), SimulationMode.SINGLE_GAME_RUN);

        // then
        assertAll(() -> assertEquals(SimulationMode.SINGLE_GAME_RUN, published.getFirst().mode()));
    }

    @Test
    @DisplayName("結果をtimeoutまで受信できない場合は待機を削除する")
    void removesWaitAfterTimeout() {
        // given
        var registry = new WaitingResultRegistry();
        var coordinator =
                new SimulationCoordinator(request -> {}, registry, limits(Duration.ofMillis(1)));

        // when
        var exception =
                assertThrows(
                        SimulationTimeoutException.class, () -> coordinator.simulate(players(9)));

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
                        request -> {
                            throw failure;
                        },
                        registry,
                        limits(Duration.ofSeconds(1)));

        // when
        var exception =
                assertThrows(SimulationSendException.class, () -> coordinator.simulate(players(9)));

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
                new SimulationCoordinator(request -> {}, registry, limits(Duration.ofSeconds(5)));
        Thread thread =
                Thread.ofPlatform()
                        .unstarted(
                                () -> {
                                    try {
                                        coordinator.simulate(players(9));
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
    @DisplayName("入力選手が8件の場合はSQS送信をせず拒否する")
    void rejectsEightPlayers() {
        // given
        var coordinator =
                new SimulationCoordinator(
                        request -> {}, new WaitingResultRegistry(), limits(Duration.ofSeconds(1)));

        // when
        var exception =
                assertThrows(
                        IllegalArgumentException.class, () -> coordinator.simulate(players(8)));

        // then
        assertAll(
                () ->
                        assertEquals(
                                "players must contain exactly 9 entries", exception.getMessage()));
    }

    @Test
    @DisplayName("入力選手が10件の場合はSQS送信をせず拒否する")
    void rejectsTenPlayers() {
        // given
        var coordinator =
                new SimulationCoordinator(
                        request -> {}, new WaitingResultRegistry(), limits(Duration.ofSeconds(1)));

        // when
        var exception =
                assertThrows(
                        IllegalArgumentException.class, () -> coordinator.simulate(players(10)));

        // then
        assertAll(
                () ->
                        assertEquals(
                                "players must contain exactly 9 entries", exception.getMessage()));
    }

    @Test
    @DisplayName("9人の出塁率平均が上限を超える場合はSQS送信をせず拒否する")
    void rejectsLineupWithExcessiveHitAverage() {
        // given
        var coordinator =
                new SimulationCoordinator(
                        request -> {}, new WaitingResultRegistry(), limits(Duration.ofMillis(1)));
        var excessivePlayers =
                java.util.stream.IntStream.range(0, 9)
                        .mapToObj(
                                _ ->
                                        PlayerDataBuilder.playerData()
                                                .name("山田")
                                                .hitAverage(0.351f)
                                                .buntEnabled(true)
                                                .stealEnabled(true)
                                                .personality(PlayerPersonality.DEFAULT)
                                                .build())
                        .toList();

        // when
        var exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> coordinator.simulate(excessivePlayers));

        // then
        assertAll(
                () ->
                        assertEquals(
                                "the average hitAverage must not exceed 0.35",
                                exception.getMessage()));
    }
}

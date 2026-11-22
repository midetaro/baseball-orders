package com.example.baseballorders.backend.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.*;

import com.example.baseballorders.backend.application.SimulationCoordinator;
import com.example.baseballorders.backend.application.SimulationLimits;
import com.example.baseballorders.backend.application.SimulationLimitsBuilder;
import com.example.baseballorders.backend.application.adapter.SimulatorClient;
import com.example.baseballorders.backend.application.dto.SimulationRequest;
import com.example.baseballorders.backend.application.exception.SimulationSendException;
import com.example.baseballorders.backend.application.exception.SimulationTimeoutException;
import com.example.baseballorders.backend.domain.PlayerData;
import com.example.baseballorders.backend.domain.PlayerDataBuilder;
import com.example.baseballorders.backend.domain.PlayerPersonality;
import com.example.baseballorders.backend.domain.SimulationMode;
import com.example.baseballorders.backend.domain.SimulationResult;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SimulationCoordinatorTest {

    /** application.ymlの既定値と同じ上限を組み立てる。 */
    private static SimulationLimits limits() {
        return SimulationLimitsBuilder.simulationLimits().maximumAverageHitAverage(0.350f).build();
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

    /** 要求を記録し、同じsimulation IDの結果を同期的に返すsimulatorの代役。 */
    private static SimulatorClient recordingClient(List<SimulationRequest> requests) {
        return request -> {
            requests.add(request);
            return new SimulationResult(
                    request.simulationId(), new SimulationResult.Statistics(5, 5, 5));
        };
    }

    /** 呼び出されたら失敗させ、検証で拒否された要求がsimulatorへ届かないことを示すsimulatorの代役。 */
    private static SimulatorClient unreachableClient() {
        return _ -> {
            throw new AssertionError("simulator must not be called");
        };
    }

    @Test
    @DisplayName("画面入力の選手を要求へ変換してsimulatorへ送り、その応答を結果として返す")
    void sendsRequestAndReturnsSimulatorResult() {
        // given
        var requests = new ArrayList<SimulationRequest>();
        var coordinator = new SimulationCoordinator(recordingClient(requests), limits());

        // when
        SimulationResult result = coordinator.simulate(players(9));

        // then
        assertAll(
                () -> assertEquals(1, requests.size()),
                () -> assertEquals(result.simulationId(), requests.getFirst().simulationId()),
                () -> assertEquals("山田", requests.getFirst().players().getFirst().name()),
                () -> assertEquals(0.301f, requests.getFirst().players().getFirst().hitAverage()),
                () -> assertTrue(requests.getFirst().players().getFirst().buntEnabled()),
                () -> assertTrue(requests.getFirst().players().getFirst().stealEnabled()),
                () -> assertEquals(5, result.statistics().maximumScore()),
                () -> assertEquals(SimulationMode.LARGE_SCALE_RUN, requests.getFirst().mode()));
    }

    @Test
    @DisplayName("要求ごとに異なるsimulation IDを割り当てる")
    void assignsDistinctSimulationIds() {
        // given
        var requests = new ArrayList<SimulationRequest>();
        var coordinator = new SimulationCoordinator(recordingClient(requests), limits());

        // when
        coordinator.simulate(players(9));
        coordinator.simulate(players(9));

        // then
        assertAll(
                () ->
                        assertNotEquals(
                                requests.get(0).simulationId(), requests.get(1).simulationId()));
    }

    @Test
    @DisplayName("1試合実行モードを指定すると要求に1試合実行モードが設定される")
    void sendsSingleGameModeWhenRequested() {
        // given
        var requests = new ArrayList<SimulationRequest>();
        var coordinator = new SimulationCoordinator(recordingClient(requests), limits());

        // when
        coordinator.simulate(players(9), SimulationMode.SINGLE_GAME_RUN);

        // then
        assertAll(() -> assertEquals(SimulationMode.SINGLE_GAME_RUN, requests.getFirst().mode()));
    }

    @Test
    @DisplayName("simulatorが時間内に応答しない場合はtimeout例外をそのまま返す")
    void propagatesTimeout() {
        // given
        var timeout = new SimulationTimeoutException(UUID.randomUUID());
        SimulatorClient client =
                _ -> {
                    throw timeout;
                };
        var coordinator = new SimulationCoordinator(client, limits());

        // when
        var exception =
                assertThrows(
                        SimulationTimeoutException.class, () -> coordinator.simulate(players(9)));

        // then
        assertAll(() -> assertSame(timeout, exception));
    }

    @Test
    @DisplayName("simulatorへの送信に失敗した場合は送信失敗例外をそのまま返す")
    void propagatesSendFailure() {
        // given
        var failure =
                new SimulationSendException(
                        UUID.randomUUID(), new IllegalStateException("unavailable"));
        SimulatorClient client =
                _ -> {
                    throw failure;
                };
        var coordinator = new SimulationCoordinator(client, limits());

        // when
        var exception =
                assertThrows(SimulationSendException.class, () -> coordinator.simulate(players(9)));

        // then
        assertAll(() -> assertSame(failure, exception));
    }

    @Test
    @DisplayName("入力選手が8件の場合はsimulatorへ送らず拒否する")
    void rejectsEightPlayers() {
        // given
        var coordinator = new SimulationCoordinator(unreachableClient(), limits());

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
    @DisplayName("入力選手が10件の場合はsimulatorへ送らず拒否する")
    void rejectsTenPlayers() {
        // given
        var coordinator = new SimulationCoordinator(unreachableClient(), limits());

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
    @DisplayName("9人の出塁率平均が上限を超える場合はsimulatorへ送らず拒否する")
    void rejectsLineupWithExcessiveHitAverage() {
        // given
        var coordinator = new SimulationCoordinator(unreachableClient(), limits());
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

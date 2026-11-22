package com.example.baseballorders.backend.infrastructure.api;

import static org.junit.jupiter.api.Assertions.*;

import com.example.baseballorders.backend.application.SimulationCoordinator;
import com.example.baseballorders.backend.application.SimulationLimits;
import com.example.baseballorders.backend.application.SimulationLimitsBuilder;
import com.example.baseballorders.backend.application.dto.SimulationRequest;
import com.example.baseballorders.backend.domain.GameTransitionBuilder;
import com.example.baseballorders.backend.domain.PlayerPersonality;
import com.example.baseballorders.backend.domain.SimulationMode;
import com.example.baseballorders.backend.domain.SimulationResult;
import com.example.baseballorders.backend.domain.SimulationResultBuilder;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;

class SimulatorRequestControllerTest {

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
        "true,true",
        "true,false",
        "false,true",
        "false,false"
    })
    @DisplayName("画面の盗塁・バント選択と性格をsimulatorへの要求まで保持する")
    void propagatesStrategyOptions(boolean stealEnabled, boolean buntEnabled) {
        // given
        var requests = new java.util.ArrayList<SimulationRequest>();
        var controller =
                new SimulatorRequestController(
                        new SimulationCoordinator(
                                request -> {
                                    requests.add(request);
                                    return new SimulationResult(
                                            request.simulationId(),
                                            new SimulationResult.Statistics(0, 0, 0));
                                },
                                limits()));

        // when
        controller.send(
                playersWith(
                        new PlayerInputRequest(
                                0.3f, buntEnabled, stealEnabled, PlayerPersonality.EAGER_STEAL)));

        // then
        var players = requests.getFirst().players();
        assertAll(
                () -> assertEquals(stealEnabled, players.getFirst().stealEnabled()),
                () -> assertEquals(buntEnabled, players.getFirst().buntEnabled()),
                () -> assertEquals(PlayerPersonality.EAGER_STEAL, players.getFirst().personality()),
                () -> assertEquals("1番", players.getFirst().name()),
                () -> assertEquals("9番", players.getLast().name()),
                () -> assertEquals(9, players.size()));
    }

    @Test
    @DisplayName("画面入力した打順データをsimulatorの応答を待機して同期的に返す")
    void returnsSynchronousSimulationResult() {
        // given
        var coordinator =
                new SimulationCoordinator(
                        request ->
                                new SimulationResult(
                                        request.simulationId(),
                                        new SimulationResult.Statistics(5, 5, 5)),
                        limits());
        var controller = new SimulatorRequestController(coordinator);

        // when
        SimulationResult result =
                controller.send(
                        java.util.stream.IntStream.rangeClosed(1, 9)
                                .mapToObj(number -> new PlayerInputRequest(0.300f, true, true))
                                .toList());

        // then
        assertAll(
                () -> assertEquals(5, result.statistics().averageScore()),
                () -> assertEquals(5, result.statistics().maximumScore()));
    }

    @Test
    @DisplayName("1試合実行エンドポイントは1試合実行モードで要求し状況推移を含む結果を返す")
    void returnsSingleGameResultWithTransitions() {
        // given
        var requestedMode = new java.util.concurrent.atomic.AtomicReference<SimulationMode>();
        var coordinator =
                new SimulationCoordinator(
                        request -> {
                            requestedMode.set(request.mode());
                            return SimulationResultBuilder.simulationResult()
                                    .simulationId(request.simulationId())
                                    .statistics(new SimulationResult.Statistics(5, 5, 5))
                                    .transitions(
                                            List.of(
                                                    GameTransitionBuilder.gameTransition()
                                                            .inning(1)
                                                            .actionResult("中前安打")
                                                            .outCount(0)
                                                            .cumulativeScore(0)
                                                            .runnerState("一塁")
                                                            .build()))
                                    .build();
                        },
                        limits());
        var controller = new SimulatorRequestController(coordinator);

        // when
        SimulationResult result =
                controller.sendSingleGame(
                        java.util.stream.IntStream.rangeClosed(1, 9)
                                .mapToObj(number -> new PlayerInputRequest(0.300f, true, true))
                                .toList());

        // then
        assertAll(
                () -> assertEquals(SimulationMode.SINGLE_GAME_RUN, requestedMode.get()),
                () -> assertEquals(1, result.transitions().size()),
                () -> assertEquals("中前安打", result.transitions().getFirst().actionResult()));
    }

    @Test
    @DisplayName("範囲外または未入力の打撃データを拒否する")
    void rejectsInvalidPlayerInput() {
        // given
        var controller =
                new SimulatorRequestController(
                        new SimulationCoordinator(
                                _ -> {
                                    throw new AssertionError("simulator must not be called");
                                },
                                limits()));

        // when
        var hitAverageException =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                controller.send(
                                        playersWith(
                                                new PlayerInputRequest(-0.001f, false, false))));
        var missingValueException =
                assertThrows(
                        NullPointerException.class,
                        () ->
                                controller.send(
                                        playersWith(new PlayerInputRequest(null, false, false))));

        // then
        assertAll(
                () ->
                        assertEquals(
                                "hitAverage must be between 0.0 and 1.0",
                                hitAverageException.getMessage()),
                () ->
                        assertEquals(
                                "hit_average must not be null",
                                missingValueException.getMessage()));
    }

    @Test
    @DisplayName("POST APIは202ではなく結果を返す通常の同期エンドポイントである")
    void exposesSynchronousPostEndpoint() throws NoSuchMethodException {
        // given
        var method = SimulatorRequestController.class.getMethod("send", List.class);

        // when
        var postMapping = method.getAnnotation(PostMapping.class);

        // then
        assertAll(
                () -> assertEquals(PostMapping.class, postMapping.annotationType()),
                () -> assertEquals(SimulationResult.class, method.getReturnType()));
    }

    /** application.ymlの既定値と同じ上限を組み立てる。 */
    private static SimulationLimits limits() {
        return SimulationLimitsBuilder.simulationLimits().maximumAverageHitAverage(0.350f).build();
    }

    private List<PlayerInputRequest> playersWith(PlayerInputRequest player) {
        return java.util.stream.IntStream.range(0, 9).mapToObj(_ -> player).toList();
    }
}

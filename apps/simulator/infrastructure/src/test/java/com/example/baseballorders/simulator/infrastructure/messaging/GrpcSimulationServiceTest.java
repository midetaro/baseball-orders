package com.example.baseballorders.simulator.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.baseballorders.messaging.PlayerPersonality;
import com.example.baseballorders.messaging.SimulationMode;
import com.example.baseballorders.messaging.SimulationPlayerMessage;
import com.example.baseballorders.messaging.SimulationRequestMessage;
import com.example.baseballorders.messaging.SimulationResultMessage;
import com.example.baseballorders.messaging.SimulationServiceGrpc;
import com.example.baseballorders.simulator.application.contract.SimulationResponse;
import com.example.baseballorders.simulator.application.contract.SimulationResponseBuilder;
import com.example.baseballorders.simulator.application.contract.SimulationResult;
import com.example.baseballorders.simulator.application.contract.SimulationResultBuilder;
import com.example.baseballorders.simulator.application.usecase.SimulateGameUseCase;
import com.example.baseballorders.simulator.application.usecase.SimulationRunMode;
import com.example.baseballorders.simulator.domain.player.LineUpEntity;
import com.example.baseballorders.simulator.domain.statistics.GameStatisticsBuilder;
import com.example.baseballorders.simulator.domain.statistics.GameTransition;
import com.example.baseballorders.simulator.domain.statistics.GameTransitionBuilder;
import com.example.baseballorders.simulator.domain.statistics.ScoreAccumulator;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * 実物: GrpcSimulationService、gRPCのインプロセスサーバー・チャネル、メッセージの直列化。 モック: SimulateGameUseCase と
 * LineUpMapper。 担保する疎通: gRPCクライアント -> GrpcSimulationService -> LineUpMapper -> SimulateGameUseCase
 * -> 結果メッセージ。 担保しないもの: ネットワーク越しの通信、試合計算の中身、打順の変換規則。
 */
class GrpcSimulationServiceTest {

    private final SimulateGameUseCase useCase = mock(SimulateGameUseCase.class);
    private final LineUpMapper mapper = mock(LineUpMapper.class);
    private final LineUpEntity lineUp = mock(LineUpEntity.class);

    @Test
    @DisplayName("大規模実行の要求は全試合の統計を要求IDつきで返す")
    void returnsStatisticsEchoingSimulationId() throws Exception {
        // given
        var simulationId = UUID.randomUUID().toString();
        var request = request(simulationId, SimulationMode.SIMULATION_MODE_LARGE_SCALE_RUN);
        when(mapper.map(any())).thenReturn(lineUp);
        when(useCase.invoke(eq(lineUp), eq(SimulationRunMode.LARGE_SCALE_RUN)))
                .thenReturn(simulationResult(List.of()));
        var sut = new GrpcSimulationService(useCase, mapper);

        // when
        SimulationResultMessage result = call(sut, request);

        // then
        var score = result.getGameScoreStatistics();
        var content = result.getGameContentStatistics();
        var playersCaptor = ArgumentCaptor.forClass(List.class);
        verify(mapper).map(playersCaptor.capture());
        assertAll(
                () -> assertEquals(simulationId, result.getSimulationId()),
                () -> assertEquals(9, playersCaptor.getValue().size()),
                () -> assertEquals(4.5, score.getAverageScore(), 0.0001),
                () -> assertEquals(4.5, score.getMedianScore(), 0.0001),
                () -> assertEquals(9, score.getMaximumScore()),
                () -> assertEquals(10, score.getGameCount()),
                () -> assertEquals(10, score.getScoreDistributionCount()),
                () -> assertEquals(1, score.getScoreDistributionOrThrow(3)),
                () -> assertEquals(100, content.getHitCount()),
                () -> assertEquals(10, content.getSingleHitCount()),
                () -> assertEquals(20, content.getDoubleHitCount()),
                () -> assertEquals(30, content.getTripleHitCount()),
                () -> assertEquals(40, content.getHomeRunCount()),
                () -> assertEquals(20, content.getSoloHomeRunCount()),
                () -> assertEquals(30, content.getTwoRunHomeRunCount()),
                () -> assertEquals(50, content.getThreeRunHomeRunCount()),
                () -> assertEquals(70, content.getGrandSlamCount()),
                () -> assertEquals(110, content.getBuntCount()),
                () -> assertEquals(130, content.getStealCount()),
                () -> assertEquals(170, content.getBuntFailureCount()),
                () -> assertEquals(190, content.getStealFailureCount()),
                () -> assertEquals(230, content.getAdvancingBuntCount()),
                () -> assertEquals(290, content.getSqueezeBuntCount()),
                () -> assertEquals(310, content.getAdvancingBuntFailureCount()),
                () -> assertEquals(370, content.getSqueezeBuntFailureCount()),
                () -> assertEquals(410, content.getStealToSecondCount()),
                () -> assertEquals(430, content.getStealToThirdCount()),
                () -> assertEquals(0, result.getGameTransitionsCount()));
    }

    @Test
    @DisplayName("1試合実行の要求は状況推移を結果へ含める")
    void returnsTransitionsForSingleGameRun() throws Exception {
        // given
        var request = request("single", SimulationMode.SIMULATION_MODE_SINGLE_GAME_RUN);
        GameTransition transition =
                GameTransitionBuilder.gameTransition()
                        .inning(1)
                        .actionResult("四球")
                        .outCount(2)
                        .cumulativeScore(3)
                        .runnerState("一塁に走者")
                        .build();
        when(mapper.map(any())).thenReturn(lineUp);
        when(useCase.invoke(eq(lineUp), eq(SimulationRunMode.SINGLE_GAME_RUN)))
                .thenReturn(simulationResult(List.of(transition)));
        var sut = new GrpcSimulationService(useCase, mapper);

        // when
        SimulationResultMessage result = call(sut, request);

        // then
        var message = result.getGameTransitions(0);
        assertAll(
                () -> assertEquals(1, result.getGameTransitionsCount()),
                () -> assertEquals(1, message.getInning()),
                () -> assertEquals("四球", message.getActionResult()),
                () -> assertEquals(2, message.getOutCount()),
                () -> assertEquals(3, message.getCumulativeScore()),
                () -> assertEquals("一塁に走者", message.getRunnerState()));
    }

    @Test
    @DisplayName("認識できない実行モードはINVALID_ARGUMENTで拒否しユースケースを呼ばない")
    void rejectsUnrecognizedMode() {
        // given
        var request =
                SimulationRequestMessage.newBuilder()
                        .setSimulationId("bad-mode")
                        .setModeValue(99)
                        .build();
        when(mapper.map(any())).thenReturn(lineUp);
        var sut = new GrpcSimulationService(useCase, mapper);

        // when
        var exception = assertThrows(StatusRuntimeException.class, () -> call(sut, request));

        // then
        assertAll(
                () -> assertEquals(Status.Code.INVALID_ARGUMENT, exception.getStatus().getCode()),
                () -> org.mockito.Mockito.verifyNoInteractions(useCase));
    }

    @Test
    @DisplayName("ユースケースが不正な引数を報告するとINVALID_ARGUMENTで失敗させる")
    void mapsIllegalArgumentToInvalidArgument() {
        // given
        var request = request("bad-lineup", SimulationMode.SIMULATION_MODE_LARGE_SCALE_RUN);
        when(mapper.map(any())).thenReturn(lineUp);
        when(useCase.invoke(any(LineUpEntity.class), any(SimulationRunMode.class)))
                .thenThrow(new IllegalArgumentException("LineUpEntity size must be 9"));
        var sut = new GrpcSimulationService(useCase, mapper);

        // when
        var exception = assertThrows(StatusRuntimeException.class, () -> call(sut, request));

        // then
        assertAll(
                () -> assertEquals(Status.Code.INVALID_ARGUMENT, exception.getStatus().getCode()),
                () ->
                        assertEquals(
                                "LineUpEntity size must be 9",
                                exception.getStatus().getDescription()));
    }

    @Test
    @DisplayName("予期しない実行時例外はINTERNALで失敗させ内部情報を漏らさない")
    void mapsUnexpectedFailureToInternal() {
        // given
        var request = request("boom", SimulationMode.SIMULATION_MODE_LARGE_SCALE_RUN);
        when(mapper.map(any())).thenReturn(lineUp);
        when(useCase.invoke(any(LineUpEntity.class), any(SimulationRunMode.class)))
                .thenThrow(new IllegalStateException("secret detail"));
        var sut = new GrpcSimulationService(useCase, mapper);

        // when
        var exception = assertThrows(StatusRuntimeException.class, () -> call(sut, request));

        // then
        assertAll(
                () -> assertEquals(Status.Code.INTERNAL, exception.getStatus().getCode()),
                () -> assertEquals("Simulation failed", exception.getStatus().getDescription()));
    }

    private static SimulationRequestMessage request(String simulationId, SimulationMode mode) {
        return SimulationRequestMessage.newBuilder()
                .setSimulationId(simulationId)
                .addAllPlayers(
                        Collections.nCopies(
                                9,
                                SimulationPlayerMessage.newBuilder()
                                        .setName("player")
                                        .setHitAverage(0.3f)
                                        .setPersonality(
                                                PlayerPersonality.PLAYER_PERSONALITY_DEFAULT)
                                        .build()))
                .setMode(mode)
                .build();
    }

    private static SimulationResult simulationResult(List<GameTransition> transitions) {
        ScoreAccumulator accumulator = new ScoreAccumulator();
        List<SimulationResponse> responses =
                IntStream.range(0, 10)
                        .mapToObj(
                                index ->
                                        SimulationResponseBuilder.simulationResponse()
                                                .score(index)
                                                .runs(4)
                                                .gameStatistics(
                                                        GameStatisticsBuilder.gameStatistics()
                                                                .hitCount(10)
                                                                .singleHitCount(1)
                                                                .doubleHitCount(2)
                                                                .tripleHitCount(3)
                                                                .homeRunCount(4)
                                                                .soloHomeRunCount(2)
                                                                .twoRunHomeRunCount(3)
                                                                .threeRunHomeRunCount(5)
                                                                .grandSlamCount(7)
                                                                .buntCount(11)
                                                                .stealCount(13)
                                                                .buntFailureCount(17)
                                                                .stealFailureCount(19)
                                                                .advancingBuntCount(23)
                                                                .squeezeBuntCount(29)
                                                                .advancingBuntFailureCount(31)
                                                                .squeezeBuntFailureCount(37)
                                                                .stealToSecondCount(41)
                                                                .stealToThirdCount(43)
                                                                .build())
                                                .build())
                        .toList();
        responses.forEach(
                response ->
                        accumulator.onGameCompleted(response.score(), response.gameStatistics()));
        return SimulationResultBuilder.simulationResult()
                .statistics(accumulator.toScoreStatistics())
                .transitions(transitions)
                .build();
    }

    private static SimulationResultMessage call(
            GrpcSimulationService service, SimulationRequestMessage request) throws Exception {
        String name = "simulation-" + UUID.randomUUID();
        Server server =
                InProcessServerBuilder.forName(name).directExecutor().addService(service).build();
        server.start();
        ManagedChannel channel = InProcessChannelBuilder.forName(name).directExecutor().build();
        try {
            return SimulationServiceGrpc.newBlockingStub(channel).simulate(request);
        } finally {
            channel.shutdownNow();
            server.shutdownNow();
        }
    }
}

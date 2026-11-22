package com.example.baseballorders.backend.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.*;

import com.example.baseballorders.backend.application.dto.SimulationRequest;
import com.example.baseballorders.backend.application.dto.SimulationRequestBuilder;
import com.example.baseballorders.backend.application.exception.SimulationSendException;
import com.example.baseballorders.backend.application.exception.SimulationTimeoutException;
import com.example.baseballorders.backend.domain.PlayerData;
import com.example.baseballorders.backend.domain.PlayerDataBuilder;
import com.example.baseballorders.backend.domain.PlayerPersonality;
import com.example.baseballorders.backend.domain.SimulationMode;
import com.example.baseballorders.messaging.GameContentStatistics;
import com.example.baseballorders.messaging.GameScoreStatistics;
import com.example.baseballorders.messaging.GameTransitionMessage;
import com.example.baseballorders.messaging.PitcherPersonality;
import com.example.baseballorders.messaging.SimulationRequestMessage;
import com.example.baseballorders.messaging.SimulationResultMessage;
import com.example.baseballorders.messaging.SimulationServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GrpcSimulatorClientTest {

    private static final UUID SIMULATION_ID =
            UUID.fromString("7c4c2f0a-55f1-4c39-9e0e-0d8b0b3a8f51");

    private Server server;
    private ManagedChannel channel;

    @AfterEach
    void shutDown() throws InterruptedException {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

    /**
     * 指定した応答処理を持つsimulatorの代役をin-process gRPCサーバーとして起動し、そこへ接続するClientを返す。
     *
     * @param behavior 受信した要求に対する代役simulatorの応答処理
     * @param resultTimeout Clientに与える応答待機時間
     * @return 代役simulatorへ接続したClient
     */
    private GrpcSimulatorClient startClient(
            BiConsumer<SimulationRequestMessage, StreamObserver<SimulationResultMessage>> behavior,
            Duration resultTimeout)
            throws IOException {
        String serverName = InProcessServerBuilder.generateName();
        server =
                InProcessServerBuilder.forName(serverName)
                        .directExecutor()
                        .addService(
                                new SimulationServiceGrpc.SimulationServiceImplBase() {
                                    @Override
                                    public void simulate(
                                            SimulationRequestMessage request,
                                            StreamObserver<SimulationResultMessage> observer) {
                                        behavior.accept(request, observer);
                                    }
                                })
                        .build()
                        .start();
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        return new GrpcSimulatorClient(channel, resultTimeout);
    }

    private static SimulationRequest request(SimulationMode mode) {
        List<PlayerData> players =
                List.of(
                        player("1番", 0.301f, true, false, PlayerPersonality.DEFAULT),
                        player("2番", 0.250f, false, true, PlayerPersonality.MIDDLE_DISTANCE),
                        player("3番", 0.280f, false, false, PlayerPersonality.EAGER_SLUGGISH),
                        player("4番", 0.290f, false, false, PlayerPersonality.HIGH_ON_BASE),
                        player("5番", 0.270f, false, true, PlayerPersonality.EAGER_STEAL),
                        player("6番", 0.260f, true, false, PlayerPersonality.EAGER_BUNT),
                        player("7番", 0.240f, false, false, PlayerPersonality.DEFAULT),
                        player("8番", 0.230f, false, false, PlayerPersonality.DEFAULT),
                        player("9番", 0.220f, false, false, PlayerPersonality.DEFAULT));
        return SimulationRequestBuilder.simulationRequest()
                .simulationId(SIMULATION_ID)
                .players(players)
                .mode(mode)
                .build();
    }

    private static PlayerData player(
            String name,
            float hitAverage,
            boolean buntEnabled,
            boolean stealEnabled,
            PlayerPersonality personality) {
        return PlayerDataBuilder.playerData()
                .name(name)
                .hitAverage(hitAverage)
                .buntEnabled(buntEnabled)
                .stealEnabled(stealEnabled)
                .personality(personality)
                .build();
    }

    private static SimulationResultMessage resultMessage(String simulationId) {
        return SimulationResultMessage.newBuilder()
                .setSimulationId(simulationId)
                .setGameScoreStatistics(
                        GameScoreStatistics.newBuilder()
                                .setAverageScore(6.5)
                                .setMedianScore(6.0)
                                .setMaximumScore(8)
                                .setGameCount(2)
                                .putScoreDistribution(5, 1)
                                .putScoreDistribution(8, 1))
                .setGameContentStatistics(
                        GameContentStatistics.newBuilder()
                                .setHitCount(1)
                                .setSingleHitCount(2)
                                .setDoubleHitCount(3)
                                .setTripleHitCount(4)
                                .setHomeRunCount(5)
                                .setSoloHomeRunCount(6)
                                .setTwoRunHomeRunCount(7)
                                .setThreeRunHomeRunCount(8)
                                .setGrandSlamCount(9)
                                .setBuntCount(10)
                                .setStealCount(11)
                                .setBuntFailureCount(12)
                                .setStealFailureCount(13)
                                .setAdvancingBuntCount(14)
                                .setSqueezeBuntCount(15)
                                .setAdvancingBuntFailureCount(16)
                                .setSqueezeBuntFailureCount(17)
                                .setStealToSecondCount(18)
                                .setStealToThirdCount(19))
                .addGameTransitions(
                        GameTransitionMessage.newBuilder()
                                .setInning(3)
                                .setActionResult("中前安打")
                                .setOutCount(1)
                                .setCumulativeScore(2)
                                .setRunnerState("一塁"))
                .build();
    }

    @Test
    @DisplayName("backendの要求を共有gRPCメッセージへ変換してsimulatorへ送る")
    void mapsRequestToContractMessage() throws IOException {
        // given
        var received = new AtomicReference<SimulationRequestMessage>();
        var client =
                startClient(
                        (message, observer) -> {
                            received.set(message);
                            observer.onNext(resultMessage(message.getSimulationId()));
                            observer.onCompleted();
                        },
                        Duration.ofSeconds(5));

        // when
        client.simulate(request(SimulationMode.SINGLE_GAME_RUN));

        // then
        var message = received.get();
        assertAll(
                () -> assertEquals(SIMULATION_ID.toString(), message.getSimulationId()),
                () ->
                        assertEquals(
                                com.example.baseballorders.messaging.SimulationMode
                                        .SIMULATION_MODE_SINGLE_GAME_RUN,
                                message.getMode()),
                () ->
                        assertEquals(
                                PitcherPersonality.PITCHER_PERSONALITY_DEFAULT,
                                message.getPitcherPersonality()),
                () -> assertEquals(9, message.getPlayersCount()),
                () -> assertEquals("1番", message.getPlayers(0).getName()),
                () -> assertEquals(0.301f, message.getPlayers(0).getHitAverage()),
                () -> assertTrue(message.getPlayers(0).getBuntEnabled()),
                () -> assertFalse(message.getPlayers(0).getStealEnabled()),
                () -> assertTrue(message.getPlayers(1).getStealEnabled()),
                () ->
                        assertEquals(
                                List.of(
                                        com.example.baseballorders.messaging.PlayerPersonality
                                                .PLAYER_PERSONALITY_DEFAULT,
                                        com.example.baseballorders.messaging.PlayerPersonality
                                                .PLAYER_PERSONALITY_MIDDLE_DISTANCE,
                                        com.example.baseballorders.messaging.PlayerPersonality
                                                .PLAYER_PERSONALITY_EAGER_SLUGGISH,
                                        com.example.baseballorders.messaging.PlayerPersonality
                                                .PLAYER_PERSONALITY_HIGH_ON_BASE,
                                        com.example.baseballorders.messaging.PlayerPersonality
                                                .PLAYER_PERSONALITY_EAGER_STEAL,
                                        com.example.baseballorders.messaging.PlayerPersonality
                                                .PLAYER_PERSONALITY_EAGER_BUNT),
                                message.getPlayersList().subList(0, 6).stream()
                                        .map(
                                                com.example.baseballorders.messaging
                                                                .SimulationPlayerMessage
                                                        ::getPersonality)
                                        .toList()));
    }

    @Test
    @DisplayName("大規模実行モードは共有メッセージの大規模実行モードへ変換される")
    void mapsLargeScaleMode() throws IOException {
        // given
        var received = new AtomicReference<SimulationRequestMessage>();
        var client =
                startClient(
                        (message, observer) -> {
                            received.set(message);
                            observer.onNext(resultMessage(message.getSimulationId()));
                            observer.onCompleted();
                        },
                        Duration.ofSeconds(5));

        // when
        client.simulate(request(SimulationMode.LARGE_SCALE_RUN));

        // then
        assertAll(
                () ->
                        assertEquals(
                                com.example.baseballorders.messaging.SimulationMode
                                        .SIMULATION_MODE_LARGE_SCALE_RUN,
                                received.get().getMode()));
    }

    @Test
    @DisplayName("simulatorが不正なシミュレーションIDを返しても要求のIDで結果を返す")
    void usesRequestSimulationIdEvenWhenResponseIdIsInvalid() throws IOException {
        // given
        var client =
                startClient(
                        (message, observer) -> {
                            observer.onNext(resultMessage("not-a-uuid"));
                            observer.onCompleted();
                        },
                        Duration.ofSeconds(5));

        // when
        var result = client.simulate(request(SimulationMode.LARGE_SCALE_RUN));

        // then
        assertAll(() -> assertEquals(SIMULATION_ID, result.simulationId()));
    }

    @Test
    @DisplayName("simulatorの応答をbackendのシミュレーション結果へ変換して返す")
    void mapsResultMessageToDomainResult() throws IOException {
        // given
        var client =
                startClient(
                        (message, observer) -> {
                            observer.onNext(resultMessage(message.getSimulationId()));
                            observer.onCompleted();
                        },
                        Duration.ofSeconds(5));

        // when
        var result = client.simulate(request(SimulationMode.SINGLE_GAME_RUN));

        // then
        var statistics = result.statistics();
        var transition = result.transitions().getFirst();
        assertAll(
                () -> assertEquals(SIMULATION_ID, result.simulationId()),
                () -> assertEquals(6.5, statistics.averageScore()),
                () -> assertEquals(6.0, statistics.medianScore()),
                () -> assertEquals(8, statistics.maximumScore()),
                () -> assertEquals(2, statistics.gameCount()),
                () -> assertEquals(java.util.Map.of(5, 1, 8, 1), statistics.scoreDistribution()),
                () -> assertEquals(1, statistics.hitCount()),
                () -> assertEquals(2, statistics.singleHitCount()),
                () -> assertEquals(3, statistics.doubleHitCount()),
                () -> assertEquals(4, statistics.tripleHitCount()),
                () -> assertEquals(5, statistics.homeRunCount()),
                () -> assertEquals(6, statistics.soloHomeRunCount()),
                () -> assertEquals(7, statistics.twoRunHomeRunCount()),
                () -> assertEquals(8, statistics.threeRunHomeRunCount()),
                () -> assertEquals(9, statistics.grandSlamCount()),
                () -> assertEquals(10, statistics.buntCount()),
                () -> assertEquals(11, statistics.stealCount()),
                () -> assertEquals(12, statistics.buntFailureCount()),
                () -> assertEquals(13, statistics.stealFailureCount()),
                () -> assertEquals(14, statistics.advancingBuntCount()),
                () -> assertEquals(15, statistics.squeezeBuntCount()),
                () -> assertEquals(16, statistics.advancingBuntFailureCount()),
                () -> assertEquals(17, statistics.squeezeBuntFailureCount()),
                () -> assertEquals(18, statistics.stealToSecondCount()),
                () -> assertEquals(19, statistics.stealToThirdCount()),
                () -> assertEquals(1, result.transitions().size()),
                () -> assertEquals(3, transition.inning()),
                () -> assertEquals("中前安打", transition.actionResult()),
                () -> assertEquals(1, transition.outCount()),
                () -> assertEquals(2, transition.cumulativeScore()),
                () -> assertEquals("一塁", transition.runnerState()));
    }

    @Test
    @DisplayName("得点統計を含まない応答は送信失敗として扱う")
    void rejectsResultWithoutStatistics() throws IOException {
        // given
        var client =
                startClient(
                        (message, observer) -> {
                            observer.onNext(
                                    SimulationResultMessage.newBuilder()
                                            .setSimulationId(message.getSimulationId())
                                            .build());
                            observer.onCompleted();
                        },
                        Duration.ofSeconds(5));

        // when
        var exception =
                assertThrows(
                        SimulationSendException.class,
                        () -> client.simulate(request(SimulationMode.LARGE_SCALE_RUN)));

        // then
        assertAll(() -> assertTrue(exception.getMessage().contains(SIMULATION_ID.toString())));
    }

    @Test
    @DisplayName("simulatorが応答待機時間内に応答しない場合はtimeout例外へ変換する")
    void convertsDeadlineExceededToTimeout() throws IOException {
        // given
        // 要求を受け取っても応答を返さない代役simulatorの処理
        var client = startClient((_, _) -> {}, Duration.ofMillis(50));

        // when
        var exception =
                assertThrows(
                        SimulationTimeoutException.class,
                        () -> client.simulate(request(SimulationMode.LARGE_SCALE_RUN)));

        // then
        assertAll(
                () ->
                        assertTrue(
                                exception
                                        .getMessage()
                                        .startsWith(
                                                "simulation result was not received within the allowed time:")),
                () -> assertTrue(exception.getMessage().contains(SIMULATION_ID.toString())));
    }

    @Test
    @DisplayName("simulatorへ到達できない場合は送信失敗例外へ変換する")
    void convertsUnavailableToSendFailure() throws IOException {
        // given
        var client =
                startClient(
                        (_, observer) ->
                                observer.onError(
                                        Status.UNAVAILABLE
                                                .withDescription("simulator is down")
                                                .asRuntimeException()),
                        Duration.ofSeconds(5));

        // when
        var exception =
                assertThrows(
                        SimulationSendException.class,
                        () -> client.simulate(request(SimulationMode.LARGE_SCALE_RUN)));

        // then
        assertAll(
                () -> assertTrue(exception.getMessage().contains(SIMULATION_ID.toString())),
                () ->
                        assertEquals(
                                Status.Code.UNAVAILABLE,
                                Status.fromThrowable(exception.getCause()).getCode()));
    }

    @Test
    @DisplayName("simulatorがエラー状態を返した場合は送信失敗例外へ変換する")
    void convertsErrorStatusToSendFailure() throws IOException {
        // given
        var client =
                startClient(
                        (_, observer) ->
                                observer.onError(
                                        Status.INVALID_ARGUMENT
                                                .withDescription("players must contain 9 entries")
                                                .asRuntimeException()),
                        Duration.ofSeconds(5));

        // when
        var exception =
                assertThrows(
                        SimulationSendException.class,
                        () -> client.simulate(request(SimulationMode.LARGE_SCALE_RUN)));

        // then
        assertAll(
                () ->
                        assertEquals(
                                Status.Code.INVALID_ARGUMENT,
                                Status.fromThrowable(exception.getCause()).getCode()));
    }
}

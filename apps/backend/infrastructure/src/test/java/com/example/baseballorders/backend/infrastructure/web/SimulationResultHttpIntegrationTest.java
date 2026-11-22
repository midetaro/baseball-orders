package com.example.baseballorders.backend.infrastructure.web;

import static org.junit.jupiter.api.Assertions.*;

import com.example.baseballorders.messaging.GameContentStatistics;
import com.example.baseballorders.messaging.GameScoreStatistics;
import com.example.baseballorders.messaging.GameTransitionMessage;
import com.example.baseballorders.messaging.SimulationMode;
import com.example.baseballorders.messaging.SimulationRequestMessage;
import com.example.baseballorders.messaging.SimulationResultMessage;
import com.example.baseballorders.messaging.SimulationServiceGrpc;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.ObjectMapper;

/**
 * 実物:
 * HTTPサーバー、SimulatorRequestController、SimulationCoordinator、GrpcSimulatorClient、gRPCチャネル(Netty)、
 * SimulationErrorHandler、JSON変換、ループバック上のgRPC通信。
 *
 * <p>モック: simulatorアプリケーションの代わりに、テスト内で起動する代役のSimulationService gRPCサーバー(固定の結果、エラー状態、無応答を返す)。
 *
 * <p>担保する疎通: ログインなしのHTTP JSON要求 -> SimulatorRequestController -> SimulationCoordinator ->
 * GrpcSimulatorClient -> gRPC(Netty) -> 代役SimulationService -> gRPC応答 -> GrpcSimulatorClient ->
 * HTTP JSON応答。gRPCのUNAVAILABLE -> HTTP 502、応答待機時間超過 -> HTTP 504。
 *
 * <p>担保しないもの: simulatorの実際の計算と業務ルール、実物のsimulatorプロセスとの疎通(integration-testで扱う)、ブラウザ描画。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "baseball-orders.simulation.result-timeout=2s")
class SimulationResultHttpIntegrationTest {

    private static final String NINE_PLAYERS =
            """
            [{"hit_average":0.300,"bunt_enabled":true,"steal_enabled":false,"personality":"EAGER_BUNT"},
             {"hit_average":0.300,"bunt_enabled":false,"steal_enabled":true},
             {"hit_average":0.300,"bunt_enabled":false,"steal_enabled":false},
             {"hit_average":0.300,"bunt_enabled":false,"steal_enabled":false},
             {"hit_average":0.300,"bunt_enabled":false,"steal_enabled":false},
             {"hit_average":0.300,"bunt_enabled":false,"steal_enabled":false},
             {"hit_average":0.300,"bunt_enabled":false,"steal_enabled":false},
             {"hit_average":0.300,"bunt_enabled":false,"steal_enabled":false},
             {"hit_average":0.300,"bunt_enabled":false,"steal_enabled":false}]
            """;

    /** 代役simulatorが受信した要求に対して行う応答処理。テストごとに差し替える。 */
    private static final AtomicReference<
                    BiConsumer<SimulationRequestMessage, StreamObserver<SimulationResultMessage>>>
            BEHAVIOR = new AtomicReference<>();

    /** 代役simulatorが受信した要求。 */
    private static final LinkedBlockingQueue<SimulationRequestMessage> RECEIVED =
            new LinkedBlockingQueue<>();

    private static final Server FAKE_SIMULATOR = startFakeSimulator();

    @Autowired private ObjectMapper objectMapper;
    @LocalServerPort private int port;

    private static Server startFakeSimulator() {
        try {
            return Grpc.newServerBuilderForPort(0, InsecureServerCredentials.create())
                    .addService(
                            new SimulationServiceGrpc.SimulationServiceImplBase() {
                                @Override
                                public void simulate(
                                        SimulationRequestMessage request,
                                        StreamObserver<SimulationResultMessage> observer) {
                                    RECEIVED.add(request);
                                    BEHAVIOR.get().accept(request, observer);
                                }
                            })
                    .build()
                    .start();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @DynamicPropertySource
    static void simulatorAddress(DynamicPropertyRegistry registry) {
        registry.add("baseball-orders.simulator.grpc.host", () -> "localhost");
        registry.add("baseball-orders.simulator.grpc.port", FAKE_SIMULATOR::getPort);
    }

    @AfterAll
    static void stopFakeSimulator() throws InterruptedException {
        FAKE_SIMULATOR.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

    @BeforeEach
    void clearReceived() {
        RECEIVED.clear();
    }

    private static SimulationResultMessage resultMessage(SimulationRequestMessage request) {
        var builder =
                SimulationResultMessage.newBuilder()
                        .setSimulationId(request.getSimulationId())
                        .setGameScoreStatistics(
                                GameScoreStatistics.newBuilder()
                                        .setAverageScore(6.5)
                                        .setMedianScore(6.5)
                                        .setMaximumScore(8)
                                        .setGameCount(2)
                                        .putScoreDistribution(5, 1)
                                        .putScoreDistribution(8, 1))
                        .setGameContentStatistics(
                                GameContentStatistics.newBuilder()
                                        .setHitCount(20)
                                        .setSingleHitCount(8)
                                        .setBuntCount(24)
                                        .setStealCount(25)
                                        .setStealToThirdCount(29));
        if (request.getMode() == SimulationMode.SIMULATION_MODE_SINGLE_GAME_RUN) {
            builder.addGameTransitions(
                    GameTransitionMessage.newBuilder()
                            .setInning(1)
                            .setActionResult("中前安打")
                            .setOutCount(0)
                            .setCumulativeScore(0)
                            .setRunnerState("一塁"));
        }
        return builder.build();
    }

    private HttpResponse<String> post(String path) throws IOException, InterruptedException {
        var request =
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(NINE_PLAYERS))
                        .build();
        try (var client = HttpClient.newHttpClient()) {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }

    @Test
    @DisplayName("HTTP要求をgRPCでsimulatorへ送り、その応答の統計をHTTP JSONで返す")
    void returnsSimulatorResultOverHttp() throws Exception {
        // given
        BEHAVIOR.set(
                (request, observer) -> {
                    observer.onNext(resultMessage(request));
                    observer.onCompleted();
                });

        // when
        var response = post("/simulations");

        // then
        var sent = RECEIVED.poll(5, TimeUnit.SECONDS);
        var body = objectMapper.readTree(response.body());
        var statistics = body.get("statistics");
        assertAll(
                () -> assertEquals(200, response.statusCode()),
                () -> assertEquals(SimulationMode.SIMULATION_MODE_LARGE_SCALE_RUN, sent.getMode()),
                () -> assertEquals(9, sent.getPlayersCount()),
                () -> assertEquals("1番", sent.getPlayers(0).getName()),
                () -> assertTrue(sent.getPlayers(0).getBuntEnabled()),
                () -> assertFalse(sent.getPlayers(0).getStealEnabled()),
                () ->
                        assertEquals(
                                com.example.baseballorders.messaging.PlayerPersonality
                                        .PLAYER_PERSONALITY_EAGER_BUNT,
                                sent.getPlayers(0).getPersonality()),
                () -> assertTrue(sent.getPlayers(1).getStealEnabled()),
                () -> assertEquals(sent.getSimulationId(), body.get("simulationId").asString()),
                () -> assertTrue(body.path("results").isMissingNode()),
                () -> assertEquals(6.5, statistics.get("averageScore").asDouble()),
                () -> assertEquals(6.5, statistics.get("medianScore").asDouble()),
                () -> assertEquals(8, statistics.get("maximumScore").asInt()),
                () -> assertEquals(2, statistics.get("gameCount").asInt()),
                () -> assertEquals(20, statistics.get("hitCount").asInt()),
                () -> assertEquals(8, statistics.get("singleHitCount").asInt()),
                () -> assertEquals(24, statistics.get("buntCount").asInt()),
                () -> assertEquals(25, statistics.get("stealCount").asInt()),
                () -> assertEquals(29, statistics.get("stealToThirdCount").asInt()),
                () -> assertEquals(1, statistics.get("scoreDistribution").get("5").asInt()),
                () -> assertEquals(0, body.get("transitions").size()));
    }

    @Test
    @DisplayName("1試合実行のHTTP要求は1試合実行モードでsimulatorへ送られ状況推移をHTTP JSONで返す")
    void returnsSingleGameTransitionsOverHttp() throws Exception {
        // given
        BEHAVIOR.set(
                (request, observer) -> {
                    observer.onNext(resultMessage(request));
                    observer.onCompleted();
                });

        // when
        var response = post("/simulations/single-game");

        // then
        var sent = RECEIVED.poll(5, TimeUnit.SECONDS);
        var body = objectMapper.readTree(response.body());
        var transition = body.get("transitions").get(0);
        assertAll(
                () -> assertEquals(200, response.statusCode()),
                () -> assertEquals(SimulationMode.SIMULATION_MODE_SINGLE_GAME_RUN, sent.getMode()),
                () -> assertEquals(1, body.get("transitions").size()),
                () -> assertEquals(1, transition.get("inning").asLong()),
                () -> assertEquals("中前安打", transition.get("actionResult").asString()),
                () -> assertEquals(0, transition.get("outCount").asInt()),
                () -> assertEquals("一塁", transition.get("runnerState").asString()));
    }

    @Test
    @DisplayName("simulatorがUNAVAILABLEを返した場合はHTTP 502を返す")
    void returnsBadGatewayWhenSimulatorIsUnavailable() throws Exception {
        // given
        BEHAVIOR.set(
                (_, observer) ->
                        observer.onError(
                                Status.UNAVAILABLE
                                        .withDescription("simulator is down")
                                        .asRuntimeException()));

        // when
        var response = post("/simulations");

        // then
        var body = objectMapper.readTree(response.body());
        assertAll(
                () -> assertEquals(502, response.statusCode()),
                () ->
                        assertTrue(
                                body.get("error")
                                        .asString()
                                        .startsWith("simulation request could not be sent:")));
    }

    @Test
    @DisplayName("simulatorが応答待機時間内に応答しない場合はHTTP 504を返す")
    void returnsGatewayTimeoutWhenSimulatorDoesNotRespond() throws Exception {
        // given
        // 要求を受け取っても応答を返さない代役simulatorの処理
        BEHAVIOR.set((_, _) -> {});

        // when
        var response = post("/simulations");

        // then
        var body = objectMapper.readTree(response.body());
        assertAll(
                () -> assertEquals(504, response.statusCode()),
                () ->
                        assertTrue(
                                body.get("error")
                                        .asString()
                                        .startsWith(
                                                "simulation result was not received within the allowed time:")));
    }
}

package com.example.baseballorders.integration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.baseballorders.backend.BackendApplication;
import com.example.baseballorders.simulator.SimulatorApplication;
import com.example.baseballorders.simulator.infrastructure.messaging.GrpcServerLifecycle;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.Comparator;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 実物: backendのSpring Boot(HTTPサーバー、Controller、SimulationCoordinator、GrpcSimulatorClient、gRPCチャネル、H2)、
 * simulatorのSpring Boot(GrpcServerLifecycle、GrpcSimulationService、LineUpMapper、SimulateGameUseCase、
 * application-local.ymlの確率設定)、localhost上の実ポートでのNetty/HTTP2によるgRPC通信。
 * モック: なし。
 * 担保する疎通: HTTP POST /simulations(および/simulations/single-game) -> backend Controller
 * -> SimulationCoordinator -> GrpcSimulatorClient -> gRPC(Netty/HTTP2, localhost) -> simulator GrpcSimulationService
 * -> LineUpMapper -> SimulateGameUseCase -> gRPC応答 -> backendのHTTP応答(統計、1試合実行では状況推移)。
 * 担保しないもの: AWS/ECS等の実環境のネットワーク・TLS、乱数による統計値そのものの正当性、
 * gRPCの異常系(タイムアウト・接続断・UNAVAILABLE)、ブラウザ描画。
 *
 * <p>両アプリは同名のapplication.ymlを持つため、各アプリのjarから設定を取り出して一時ディレクトリへ置き、
 * spring.config.locationで明示的に指定して読み込む。
 */
class BackendSimulatorGrpcIntegrationTest {

    private static final String PLAYER =
            "{\"hit_average\":0.300,\"bunt_enabled\":false,\"steal_enabled\":false}";
    private static final String PLAYERS =
            "[" + String.join(",", Collections.nCopies(9, PLAYER)) + "]";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static Path configRoot;
    private static ConfigurableApplicationContext simulator;
    private static ConfigurableApplicationContext backend;
    private static HttpClient http;
    private static int backendPort;

    @BeforeAll
    static void startApplications() throws IOException {
        configRoot = Files.createTempDirectory("integration-config");
        var simulatorConfig = extractConfig("simulator", SimulatorApplication.class);
        var backendConfig = extractConfig("backend", BackendApplication.class);
        simulator =
                new SpringApplicationBuilder(SimulatorApplication.class)
                        .run(
                                "--spring.profiles.active=local",
                                "--spring.config.location=file:" + simulatorConfig + "/",
                                "--simulation.grpc.port=0");
        var grpcPort = simulator.getBean(GrpcServerLifecycle.class).getPort();
        backend =
                new SpringApplicationBuilder(BackendApplication.class)
                        .run(
                                "--spring.profiles.active=local",
                                "--spring.config.location=file:" + backendConfig + "/",
                                "--server.port=0",
                                "--baseball-orders.simulator.grpc.host=localhost",
                                "--baseball-orders.simulator.grpc.port=" + grpcPort);
        backendPort = ((WebServerApplicationContext) backend).getWebServer().getPort();
        http = HttpClient.newHttpClient();
    }

    @AfterAll
    static void stopApplications() throws IOException {
        if (http != null) {
            http.close();
        }
        if (backend != null) {
            backend.close();
        }
        if (simulator != null) {
            simulator.close();
        }
        if (configRoot != null) {
            try (Stream<Path> files = Files.walk(configRoot)) {
                files.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
            }
        }
    }

    /**
     * 実物: backend HTTP/Controller/Coordinator/GrpcSimulatorClient、simulator GrpcSimulationService・LineUpMapper・
     * SimulateGameUseCase、実gRPC。
     * モック: なし。
     * 担保する疎通: HTTP POST /simulations -> gRPC -> 実試合 -> gRPC応答 -> HTTP応答(統計)。
     * 担保しないもの: 統計値の期待値、異常系、ブラウザ描画。
     */
    @Test
    @DisplayName("HTTP要求がgRPC経由で実際のsimulatorに処理され統計がHTTP応答に届く")
    void returnsStatisticsThroughGrpc() throws Exception {
        var response = post("/simulations");
        JsonNode body = MAPPER.readTree(response.body());
        var statistics = body.path("statistics");
        int scoreDistributionTotal = 0;
        for (JsonNode count : statistics.path("scoreDistribution")) {
            scoreDistributionTotal += count.asInt();
        }
        int total = scoreDistributionTotal;
        assertAll(
                () -> assertEquals(200, response.statusCode(), response.body()),
                () -> assertFalse(body.path("simulationId").asText().isBlank()),
                () -> assertEquals(100, statistics.path("gameCount").asInt(-1)),
                () -> assertEquals(100, total),
                () -> assertTrue(statistics.path("hitCount").asInt(-1) > 0),
                () -> assertTrue(statistics.path("maximumScore").asInt(-1) >= 0),
                () -> assertEquals(0, body.path("transitions").size()));
    }

    /**
     * 実物: backend HTTP/Controller/Coordinator/GrpcSimulatorClient、simulator GrpcSimulationService・LineUpMapper・
     * SimulateGameUseCase、実gRPC。
     * モック: なし。
     * 担保する疎通: HTTP POST /simulations/single-game -> gRPC -> 1試合実行 -> gRPC応答 -> HTTP応答(統計と状況推移)。
     * 担保しないもの: 状況推移の内容の正当性、異常系、ブラウザ描画。
     */
    @Test
    @DisplayName("1試合実行の要求がgRPC経由で処理され状況推移がHTTP応答に届く")
    void returnsTransitionsForSingleGameThroughGrpc() throws Exception {
        var response = post("/simulations/single-game");
        JsonNode body = MAPPER.readTree(response.body());
        assertAll(
                () -> assertEquals(200, response.statusCode(), response.body()),
                () -> assertEquals(1, body.path("statistics").path("gameCount").asInt(-1)),
                () -> assertTrue(body.path("transitions").size() > 0));
    }

    private static HttpResponse<String> post(String path) throws Exception {
        var request =
                HttpRequest.newBuilder(URI.create("http://localhost:" + backendPort + path))
                        .timeout(Duration.ofSeconds(60))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(PLAYERS))
                        .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /** 指定アプリのjar(またはクラスディレクトリ)に含まれるapplication*.ymlだけを一時ディレクトリへ取り出す。 */
    private static Path extractConfig(String name, Class<?> applicationClass) throws IOException {
        var dir = Files.createDirectories(configRoot.resolve(name));
        var location =
                applicationClass.getProtectionDomain().getCodeSource().getLocation().toString();
        for (var file : new String[] {"application.yml", "application-local.yml"}) {
            URL owned = null;
            var resources = applicationClass.getClassLoader().getResources(file);
            while (resources.hasMoreElements()) {
                var url = resources.nextElement();
                if (url.toString().contains(location)) {
                    owned = url;
                    break;
                }
            }
            if (owned == null) {
                throw new IllegalStateException(file + " was not found for " + name);
            }
            try (var in = owned.openStream()) {
                Files.copy(in, dir.resolve(file));
            }
        }
        return dir;
    }
}

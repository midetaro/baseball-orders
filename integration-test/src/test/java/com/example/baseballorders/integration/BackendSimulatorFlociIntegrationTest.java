package com.example.baseballorders.integration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.example.baseballorders.backend.BackendApplication;
import com.example.baseballorders.backend.application.WaitingResultRegistry;
import com.example.baseballorders.messaging.SimulationRequestMessage;
import com.example.baseballorders.simulator.application.LineUpMapper;
import com.example.baseballorders.simulator.application.usecase.SimulateGameUseCase;
import com.example.baseballorders.simulator.domain.code.BattingResult;
import com.example.baseballorders.simulator.domain.code.BuntResult;
import com.example.baseballorders.simulator.domain.code.StealResult;
import com.example.baseballorders.simulator.domain.model.behavior.StealStrategy;
import com.example.baseballorders.simulator.infrastructure.SqsSimulationScheduler;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.floci.testcontainers.FlociContainer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * 実物: backendのHTTPサーバー、Controller、Coordinator、H2、要求Publisher、結果SQS Listener、
 * WaitingResultRegistry、simulatorのSqsSimulationScheduler、LineUpMapper、SimulateGameUseCase、Floci SQS。
 * モック: AWS SQSをFlociに置換。打撃・盗塁・バント戦略を固定結果のテスト実装に置換。
 * 担保する疎通: HTTP POST /simulations -> backend -> 要求SQS -> simulatorの受信・試合計算
 * -> 結果SQS -> backendの自動Listener -> WaitingResultRegistry -> HTTP応答。
 * 担保しないもの: AWS実環境のIAM・ネットワーク、乱数戦略の統計的正当性、定期実行の間隔、異常系・再配信。
 * simulatorのpollはテストから起動し、SQS受信・結果送信・要求削除は本番実装を使用する。
 */
class BackendSimulatorFlociIntegrationTest {

    @Test
    @DisplayName("HTTP要求をsimulatorがFloci経由で処理しbackendが結果SQSを受信して同じ相関IDで応答する")
    void completesBackendRequestAfterSimulatorProcessesIt() throws Exception {
        // given
        var suffix = UUID.randomUUID().toString();
        var requestQueue = "simulation-request-" + suffix;
        var resultQueue = "simulation-result-" + suffix;
        try (var floci = new FlociContainer().withSqsConfig(c -> c.enabled(true)).withDockerSocket(false)) {
            floci.start();
            try (var sqs = SqsClient.builder()
                    .endpointOverride(URI.create(floci.getEndpoint()))
                    .region(Region.of(floci.getRegion()))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(floci.getAccessKey(), floci.getSecretKey())))
                    .build()) {
                var requestUrl = sqs.createQueue(r -> r.queueName(requestQueue)).queueUrl();
                sqs.createQueue(r -> r.queueName(resultQueue));
                try (var backend = new SpringApplicationBuilder(BackendApplication.class).run(
                                "--server.port=0",
                                "--spring.cloud.aws.region.static=" + floci.getRegion(),
                                "--spring.cloud.aws.credentials.access-key=" + floci.getAccessKey(),
                                "--spring.cloud.aws.credentials.secret-key=" + floci.getSecretKey(),
                                "--spring.cloud.aws.sqs.endpoint=" + floci.getEndpoint(),
                                "--spring.datasource.url=jdbc:h2:mem:" + suffix,
                                "--simulation.sqs.request-queue-name=" + requestQueue,
                                "--simulation.sqs.result-queue-name=" + resultQueue);
                        var http = HttpClient.newHttpClient()) {
                    var port = ((WebServerApplicationContext) backend).getWebServer().getPort();
                    var registry = backend.getBean(WaitingResultRegistry.class);
                    var mapper = new ObjectMapper();
                    var lineupMapper = new LineUpMapper(
                            (average, slugging) -> BattingResult.OUT,
                            new NeverStealStrategy(),
                            (rate, outs, bases) -> BuntResult.SUCCESS);
                    var simulator = new SqsSimulationScheduler(sqs, mapper,
                            new SimulateGameUseCase(Map.of(), 1), lineupMapper, requestQueue, resultQueue);
                    var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/simulations"))
                            .timeout(Duration.ofSeconds(30))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString("""
                                    [{"player_id":1},{"player_id":2},{"player_id":3},
                                     {"player_id":4},{"player_id":5},{"player_id":6},
                                     {"player_id":7},{"player_id":8},{"player_id":9}]
                                    """))
                            .build();

                    // when
                    var responseFuture = http.sendAsync(request, HttpResponse.BodyHandlers.ofString());
                    // 要求を観測して相関IDを保存し、可視性を戻して本番simulatorに処理させる。
                    var messages = sqs.receiveMessage(r -> r.queueUrl(requestUrl)
                            .waitTimeSeconds(10).maxNumberOfMessages(1)).messages();
                    assertAll(() -> assertEquals(1, messages.size(), "backendが要求SQSへ送信する"));
                    var message = messages.getFirst();
                    var wireRequest = mapper.readValue(message.body(), SimulationRequestMessage.class);
                    assertAll(() -> assertFalse(responseFuture.isDone(), "結果受信までHTTPは待機する"));
                    sqs.changeMessageVisibility(r -> r.queueUrl(requestUrl)
                            .receiptHandle(message.receiptHandle()).visibilityTimeout(0));
                    simulator.poll();
                    var response = responseFuture.get(10, TimeUnit.SECONDS);
                    var body = mapper.readTree(response.body());

                    // then
                    assertAll(
                            () -> assertEquals(200, response.statusCode(), response.body()),
                            () -> assertNotNull(wireRequest.simulationId()),
                            () -> assertEquals(wireRequest.simulationId().toString(), body.path("simulationId").asText()),
                            () -> assertEquals(9, wireRequest.players().size()),
                            () -> assertEquals(0, body.path("score").asInt(-1)),
                            () -> assertEquals(4, body.path("runs").asInt(-1)),
                            () -> assertEquals(0, registry.pendingCount()));
                }
            }
        }
    }

    private static final class NeverStealStrategy implements StealStrategy {
        @Override
        public StealResult runToDouble(float successRate) {
            return StealResult.NOT_TRY;
        }

        @Override
        public StealResult runToTriple(float successRate) {
            return StealResult.NOT_TRY;
        }
    }
}

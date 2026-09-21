package com.example.baseballorders.backend.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import com.example.baseballorders.backend.application.WaitingResultRegistry;
import com.example.baseballorders.backend.application.adapter.SimulatorMessagePublisher;
import com.example.baseballorders.backend.application.dto.SimulationRequest;
import com.example.baseballorders.backend.infrastructure.messaging.SimulationResultListener;
import com.example.baseballorders.messaging.SimulationResultMessage;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;

/**
 * 実物: HTTPサーバー、Controller、Coordinator、WaitingResultRegistry、結果Listener、JSON変換。 モック:
 * 要求送信ポートSimulatorMessagePublisher、SQS無効化時に未使用のSqsTemplate。 担保する疎通: HTTP JSON要求 -> Controller ->
 * Coordinator -> 結果Listener -> WaitingResultRegistry -> Coordinator -> Controller -> HTTP JSON応答。
 * 担保しないもの: SQS通信・メッセージ変換・削除、simulatorの計算、ブラウザ描画。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.cloud.aws.sqs.enabled=false")
class SimulationResultHttpIntegrationTest {

    @MockitoBean private SimulatorMessagePublisher publisher;
    @MockitoBean private SqsTemplate sqsTemplate;
    @Autowired private SimulationResultListener listener;
    @Autowired private WaitingResultRegistry registry;
    @Autowired private ObjectMapper objectMapper;
    @LocalServerPort private int port;

    @Test
    @DisplayName("結果が逆順に届いても同じ相関IDのHTTP要求へ得点と失点を返す")
    void returnsCorrelatedResultsToWaitingHttpRequests() throws Exception {
        // given
        var sent = new LinkedBlockingQueue<SimulationRequest>();
        doAnswer(
                        invocation -> {
                            sent.add(invocation.getArgument(0));
                            return null;
                        })
                .when(publisher)
                .publish(any());
        var request =
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/simulations"))
                        .header("Content-Type", "application/json")
                        .POST(
                                HttpRequest.BodyPublishers.ofString(
                                        """
                        [{"hit_average":0.300,"sluggish":0.450,"bunt_success_rate":0.700,"steal_success_rate":0.800,"bunt_enabled":false,"steal_enabled":false},
                         {"hit_average":0.300,"sluggish":0.450,"bunt_success_rate":0.700,"steal_success_rate":0.800,"bunt_enabled":false,"steal_enabled":false},
                         {"hit_average":0.300,"sluggish":0.450,"bunt_success_rate":0.700,"steal_success_rate":0.800,"bunt_enabled":false,"steal_enabled":false},
                         {"hit_average":0.300,"sluggish":0.450,"bunt_success_rate":0.700,"steal_success_rate":0.800,"bunt_enabled":false,"steal_enabled":false},
                         {"hit_average":0.300,"sluggish":0.450,"bunt_success_rate":0.700,"steal_success_rate":0.800,"bunt_enabled":false,"steal_enabled":false},
                         {"hit_average":0.300,"sluggish":0.450,"bunt_success_rate":0.700,"steal_success_rate":0.800,"bunt_enabled":false,"steal_enabled":false},
                         {"hit_average":0.300,"sluggish":0.450,"bunt_success_rate":0.700,"steal_success_rate":0.800,"bunt_enabled":false,"steal_enabled":false},
                         {"hit_average":0.300,"sluggish":0.450,"bunt_success_rate":0.700,"steal_success_rate":0.800,"bunt_enabled":false,"steal_enabled":false},
                         {"hit_average":0.300,"sluggish":0.450,"bunt_success_rate":0.700,"steal_success_rate":0.800,"bunt_enabled":false,"steal_enabled":false}]
                        """))
                        .build();

        // when
        try (var client = HttpClient.newHttpClient()) {
            var first = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            var firstSent = sent.poll(10, TimeUnit.SECONDS);
            assertAll(() -> assertNotNull(firstSent));
            var second = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            var secondSent = sent.poll(10, TimeUnit.SECONDS);
            assertAll(() -> assertNotNull(secondSent));
            listener.receive(
                    new SimulationResultMessage(
                            UUID.randomUUID(),
                            "1",
                            new SimulationResultMessage.Statistics(
                                    99, 99, 99, 1, Map.of(99, 1), 0, 0, 0, 0, 0, 0, 0)));
            assertAll(() -> assertFalse(first.isDone()), () -> assertFalse(second.isDone()));
            listener.receive(
                    new SimulationResultMessage(
                            secondSent.simulationId(),
                            "1",
                            new SimulationResultMessage.Statistics(
                                    0, 0, 0, 1, Map.of(0, 1), 0, 0, 0, 0, 0, 0, 0)));
            var secondResponse = second.get(10, TimeUnit.SECONDS);
            assertAll(() -> assertFalse(first.isDone()));
            listener.receive(
                    new SimulationResultMessage(
                            secondSent.simulationId(),
                            "1",
                            new SimulationResultMessage.Statistics(
                                    99, 99, 99, 1, Map.of(99, 1), 0, 0, 0, 0, 0, 0, 0)));
            listener.receive(
                    new SimulationResultMessage(
                            firstSent.simulationId(),
                            "1",
                            new SimulationResultMessage.Statistics(
                                    6.5,
                                    6.5,
                                    8,
                                    2,
                                    Map.of(5, 1, 8, 1),
                                    4,
                                    1,
                                    1,
                                    1,
                                    1,
                                    24,
                                    25,
                                    26,
                                    27,
                                    11,
                                    13,
                                    17,
                                    19,
                                    23,
                                    29)));
            var firstResponse = first.get(10, TimeUnit.SECONDS);
            var firstBody = objectMapper.readTree(firstResponse.body());
            var secondBody = objectMapper.readTree(secondResponse.body());

            // then
            assertAll(
                    () -> assertFalse(firstSent.players().getFirst().stealEnabled()),
                    () -> assertFalse(firstSent.players().getFirst().buntEnabled()),
                    () -> assertEquals(200, firstResponse.statusCode()),
                    () -> assertEquals(200, secondResponse.statusCode()),
                    () ->
                            assertEquals(
                                    firstSent.simulationId().toString(),
                                    firstBody.get("simulationId").asString()),
                    () ->
                            assertEquals(
                                    secondSent.simulationId().toString(),
                                    secondBody.get("simulationId").asString()),
                    () -> assertTrue(firstBody.path("results").isMissingNode()),
                    () ->
                            assertEquals(
                                    6.5,
                                    firstBody.get("statistics").get("averageScore").asDouble()),
                    () ->
                            assertEquals(
                                    6.5, firstBody.get("statistics").get("medianScore").asDouble()),
                    () -> assertEquals(8, firstBody.get("statistics").get("maximumScore").asInt()),
                    () -> assertEquals(2, firstBody.get("statistics").get("gameCount").asInt()),
                    () -> assertEquals(24, firstBody.get("statistics").get("buntCount").asInt()),
                    () -> assertEquals(25, firstBody.get("statistics").get("stealCount").asInt()),
                    () ->
                            assertEquals(
                                    26,
                                    firstBody.get("statistics").get("buntFailureCount").asInt()),
                    () ->
                            assertEquals(
                                    27,
                                    firstBody.get("statistics").get("stealFailureCount").asInt()),
                    () ->
                            assertEquals(
                                    11,
                                    firstBody.get("statistics").get("advancingBuntCount").asInt()),
                    () ->
                            assertEquals(
                                    13,
                                    firstBody.get("statistics").get("squeezeBuntCount").asInt()),
                    () ->
                            assertEquals(
                                    17,
                                    firstBody
                                            .get("statistics")
                                            .get("advancingBuntFailureCount")
                                            .asInt()),
                    () ->
                            assertEquals(
                                    19,
                                    firstBody
                                            .get("statistics")
                                            .get("squeezeBuntFailureCount")
                                            .asInt()),
                    () ->
                            assertEquals(
                                    23,
                                    firstBody.get("statistics").get("stealToSecondCount").asInt()),
                    () ->
                            assertEquals(
                                    29,
                                    firstBody.get("statistics").get("stealToThirdCount").asInt()),
                    () ->
                            assertEquals(
                                    1,
                                    firstBody
                                            .get("statistics")
                                            .get("scoreDistribution")
                                            .get("5")
                                            .asInt()),
                    () -> assertEquals(1, secondBody.get("statistics").get("gameCount").asInt()),
                    () -> assertEquals(0, registry.pendingCount()));
        }
    }
}

package com.example.baseballorders.backend.simulation.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import com.example.baseballorders.backend.application.WaitingResultRegistry;
import com.example.baseballorders.backend.application.adapter.SimulatorMessagePublisher;
import com.example.baseballorders.backend.application.dto.SimulationRequest;
import com.example.baseballorders.backend.simulation.infrastructure.messaging.SimulationResultListener;
import com.example.baseballorders.messaging.SimulationResultMessage;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
 * 実物: HTTPサーバー、Controller、Coordinator、H2 Repository、WaitingResultRegistry、結果Listener、JSON変換。 モック:
 * 要求送信ポートSimulatorMessagePublisher、SQS無効化時に未使用のSqsTemplate。 担保する疎通: 結果Listener ->
 * WaitingResultRegistry -> Coordinator -> Controller -> HTTP JSON応答。 担保しないもの:
 * SQS通信・メッセージ変換・削除、simulatorの計算、ブラウザ描画。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.cloud.aws.sqs.enabled=false",
            "spring.datasource.url=jdbc:h2:mem:result-http"
        })
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
                        [{"player_id":1},{"player_id":2},{"player_id":3},
                         {"player_id":4},{"player_id":5},{"player_id":6},
                         {"player_id":7},{"player_id":8},{"player_id":9}]
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
            listener.receive(new SimulationResultMessage(UUID.randomUUID(), "1", 99, 99));
            assertAll(() -> assertFalse(first.isDone()), () -> assertFalse(second.isDone()));
            listener.receive(new SimulationResultMessage(secondSent.simulationId(), "1", 0, 7));
            var secondResponse = second.get(10, TimeUnit.SECONDS);
            assertAll(() -> assertFalse(first.isDone()));
            listener.receive(new SimulationResultMessage(secondSent.simulationId(), "1", 99, 99));
            listener.receive(new SimulationResultMessage(firstSent.simulationId(), "1", 5, 4));
            var firstResponse = first.get(10, TimeUnit.SECONDS);
            var firstBody = objectMapper.readTree(firstResponse.body());
            var secondBody = objectMapper.readTree(secondResponse.body());

            // then
            assertAll(
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
                    () -> assertEquals(5, firstBody.get("score").asInt()),
                    () -> assertEquals(4, firstBody.get("runs").asInt()),
                    () -> assertEquals(0, secondBody.get("score").asInt()),
                    () -> assertEquals(7, secondBody.get("runs").asInt()),
                    () -> assertEquals(0, registry.pendingCount()));
        }
    }
}

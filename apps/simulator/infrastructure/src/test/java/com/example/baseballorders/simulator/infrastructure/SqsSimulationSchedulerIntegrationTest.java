package com.example.baseballorders.simulator.infrastructure;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.baseballorders.messaging.SimulationPlayerMessage;
import com.example.baseballorders.messaging.SimulationRequestMessage;
import com.example.baseballorders.messaging.SimulationResultMessage;
import com.example.baseballorders.simulator.application.LineUpMapper;
import com.example.baseballorders.simulator.application.contract.SimulationResponse;
import com.example.baseballorders.simulator.application.contract.SimulationResult;
import com.example.baseballorders.simulator.application.usecase.SimulateGameUseCase;
import com.example.baseballorders.simulator.domain.code.BuntResult;
import com.example.baseballorders.simulator.domain.code.OutCount;
import com.example.baseballorders.simulator.domain.code.StealResult;
import com.example.baseballorders.simulator.domain.entity.behavior.BehaviorStrategies;
import com.example.baseballorders.simulator.domain.entity.player.BatterEntity;
import com.example.baseballorders.simulator.domain.entity.player.LineUpEntity;
import com.example.baseballorders.simulator.domain.model.state.base.SingleBasesState;
import com.example.baseballorders.simulator.domain.model.statistics.GameStatisticsRecorder;
import com.example.baseballorders.simulator.domain.model.statistics.ScoreAccumulator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

class SqsSimulationSchedulerIntegrationTest {

    /**
     * 実物: SQS互換サービス（ElasticMQ/Floci）、Scheduler、ObjectMapper、LineUpMapper。 モック:
     * SimulateGameUseCase、固定の打撃・盗塁・バント戦略。 担保する疎通: request SQS -> Scheduler -> result SQS ->
     * 共有結果メッセージ、および要求削除。request SQS -> Scheduler -> LineUpMapper -> 打者の盗塁・バント選択。 担保しないもの:
     * 試合計算の正当性、backendのHTTP応答、AWS実環境。
     */
    @Disabled
    @Test
    @DisplayName("ElasticMQで受信した試合を実行すると結果を送信して要求を削除する")
    void sendsResultAndDeletesRequestWithElasticMq() throws Exception {
        // given
        ObjectMapper objectMapper = new ObjectMapper();
        SimulateGameUseCase useCase = mock(SimulateGameUseCase.class);
        List<SimulationResponse> simulationResults =
                IntStream.range(0, 10).mapToObj(index -> new SimulationResponse(index, 4)).toList();
        List<SimulationResultMessage> expectedResponses =
                List.of(
                        new SimulationResultMessage(
                                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                                "1",
                                new SimulationResultMessage.Statistics(
                                        4.5,
                                        4.5,
                                        9,
                                        10,
                                        java.util.stream.IntStream.range(0, 10)
                                                .boxed()
                                                .collect(
                                                        java.util.stream.Collectors.toMap(
                                                                java.util.function.Function
                                                                        .identity(),
                                                                ignored -> 1)),
                                        0,
                                        0,
                                        0,
                                        0,
                                        0,
                                        0,
                                        0)));
        when(useCase.invoke(any(LineUpEntity.class)))
                .thenReturn(simulationResult(simulationResults));
        LineUpMapper mapper =
                new LineUpMapper(
                        BehaviorStrategies.middleDistanceAtBat(),
                        BehaviorStrategies.eagerSteal(),
                        BehaviorStrategies.standardBunt());
        List<SimulationPlayerMessage> players =
                IntStream.rangeClosed(1, 9)
                        .mapToObj(
                                number ->
                                        new SimulationPlayerMessage(
                                                "player-" + number,
                                                0.3f,
                                                0.4f,
                                                0.7f,
                                                number % 2 == 0,
                                                0.8f,
                                                number % 3 == 0))
                        .toList();

        try (SqsClient sqsClient = createClient()) {
            String suffix = UUID.randomUUID().toString();
            String requestQueueUrl = createQueue(sqsClient, "simulation-requests-" + suffix);
            String resultQueueUrl = createQueue(sqsClient, "simulation-results-" + suffix);
            try {
                var request =
                        new SimulationRequestMessage(
                                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                                "1",
                                players);
                sqsClient.sendMessage(
                        SendMessageRequest.builder()
                                .queueUrl(requestQueueUrl)
                                .messageBody(objectMapper.writeValueAsString(request))
                                .build());
                var scheduler =
                        new SqsSimulationScheduler(
                                sqsClient,
                                objectMapper,
                                useCase,
                                mapper,
                                "simulation-requests-" + suffix,
                                "simulation-results-" + suffix);

                // when
                scheduler.poll();

                // then
                var captor = org.mockito.ArgumentCaptor.forClass(LineUpEntity.class);
                verify(useCase).invoke(captor.capture());
                var batters = captor.getValue().getBatterEntities();
                List<Message> resultMessages = receive(sqsClient, resultQueueUrl);
                List<Message> requestMessages = receive(sqsClient, requestQueueUrl);
                assertAll(
                        () ->
                                assertAll(
                                        IntStream.range(0, 9)
                                                .mapToObj(
                                                        index ->
                                                                () ->
                                                                        assertOptions(
                                                                                batters.get(index),
                                                                                (index + 1) % 3
                                                                                        == 0,
                                                                                (index + 1) % 2
                                                                                        == 0))),
                        () -> assertEquals(1, resultMessages.size()),
                        () ->
                                assertEquals(
                                        expectedResponses,
                                        resultMessages.stream()
                                                .map(
                                                        message -> {
                                                            try {
                                                                return objectMapper.readValue(
                                                                        message.body(),
                                                                        SimulationResultMessage
                                                                                .class);
                                                            } catch (Exception exception) {
                                                                throw new IllegalArgumentException(
                                                                        exception);
                                                            }
                                                        })
                                                .toList()),
                        () -> assertTrue(requestMessages.isEmpty()));
            } finally {
                deleteQueue(sqsClient, requestQueueUrl);
                deleteQueue(sqsClient, resultQueueUrl);
            }
        }
    }

    private static void assertOptions(
            BatterEntity batter, boolean stealEnabled, boolean buntEnabled) {
        var expectedSteal = stealEnabled ? StealResult.SUCCESS : StealResult.NOT_TRY;
        var expectedBunt = buntEnabled ? BuntResult.SUCCESS : BuntResult.NOT_TRY;
        assertAll(
                () ->
                        assertEquals(
                                expectedSteal,
                                batter.observedBy(new GameStatisticsRecorder()).stealToDouble()),
                () ->
                        assertEquals(
                                expectedSteal,
                                batter.observedBy(new GameStatisticsRecorder()).stealToTriple()),
                () ->
                        assertEquals(
                                expectedBunt,
                                batter.observedBy(new GameStatisticsRecorder())
                                        .bunt(OutCount.NO_OUT, new SingleBasesState(batter))));
    }

    private static SqsClient createClient() {
        return SqsClient.builder()
                .endpointOverride(URI.create(System.getenv("ELASTICMQ_ENDPOINT_URL")))
                .region(Region.US_EAST_1)
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create("test", "test")))
                .build();
    }

    private static String createQueue(SqsClient client, String queueName) {
        return client.createQueue(CreateQueueRequest.builder().queueName(queueName).build())
                .queueUrl();
    }

    private static List<Message> receive(SqsClient client, String queueUrl) {
        return client.receiveMessage(
                        ReceiveMessageRequest.builder()
                                .queueUrl(queueUrl)
                                .waitTimeSeconds(1)
                                .maxNumberOfMessages(10)
                                .build())
                .messages();
    }

    private static void deleteQueue(SqsClient client, String queueUrl) {
        client.deleteQueue(DeleteQueueRequest.builder().queueUrl(queueUrl).build());
    }

    private static SimulationResult simulationResult(List<SimulationResponse> responses) {
        ScoreAccumulator accumulator = new ScoreAccumulator();
        responses.forEach(
                response ->
                        accumulator.onGameCompleted(response.score(), response.gameStatistics()));
        return new SimulationResult(accumulator.toScoreStatistics());
    }
}

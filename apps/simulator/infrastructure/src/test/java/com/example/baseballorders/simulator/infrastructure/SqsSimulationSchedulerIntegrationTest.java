package com.example.baseballorders.simulator.infrastructure;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.baseballorders.messaging.SimulationPlayerMessage;
import com.example.baseballorders.messaging.SimulationRequestMessage;
import com.example.baseballorders.messaging.SimulationResultMessage;
import com.example.baseballorders.simulator.application.LineUpMapper;
import com.example.baseballorders.simulator.application.contract.SimulationResponse;
import com.example.baseballorders.simulator.application.usecase.SimulateGameUseCase;
import com.example.baseballorders.simulator.domain.code.BattingResult;
import com.example.baseballorders.simulator.domain.code.BuntResult;
import com.example.baseballorders.simulator.domain.code.StealResult;
import com.example.baseballorders.simulator.domain.model.behavior.StealStrategy;
import com.example.baseballorders.simulator.domain.model.player.LineUpEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
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

    @Test
    @EnabledIfEnvironmentVariable(named = "ELASTICMQ_ENDPOINT_URL", matches = ".+")
    @DisplayName("ElasticMQで受信した試合を実行すると結果を送信して要求を削除する")
    void sendsResultAndDeletesRequestWithElasticMq() throws Exception {
        // given
        ObjectMapper objectMapper = new ObjectMapper();
        SimulateGameUseCase useCase = mock(SimulateGameUseCase.class);
        List<SimulationResponse> simulationResults =
                IntStream.range(0, 10).mapToObj(index -> new SimulationResponse(index, 4)).toList();
        List<SimulationResultMessage> expectedResponses =
                IntStream.range(0, 10)
                        .mapToObj(
                                index ->
                                        new SimulationResultMessage(
                                                UUID.fromString(
                                                        "00000000-0000-0000-0000-000000000001"),
                                                "1",
                                                index,
                                                4))
                        .toList();
        when(useCase.invoke(any(LineUpEntity.class))).thenReturn(simulationResults);
        LineUpMapper mapper =
                new LineUpMapper(
                        (hitAverage, slugging) -> BattingResult.OUT,
                        new FixedStealStrategy(),
                        (successRate, outCounts, basesState) -> BuntResult.SUCCESS);
        List<SimulationPlayerMessage> players =
                IntStream.rangeClosed(1, 9)
                        .mapToObj(
                                number ->
                                        new SimulationPlayerMessage(
                                                "player-" + number, 0.3f, 0.4f, 0.7f))
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
                List<Message> resultMessages = receive(sqsClient, resultQueueUrl);
                List<Message> requestMessages = receive(sqsClient, requestQueueUrl);
                assertAll(
                        () -> assertEquals(10, resultMessages.size()),
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

    private static final class FixedStealStrategy implements StealStrategy {

        @Override
        public StealResult runToDouble() {
            return StealResult.NOT_TRY;
        }

        @Override
        public StealResult runToTriple() {
            return StealResult.NOT_TRY;
        }
    }
}

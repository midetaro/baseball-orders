package com.example.baseballorders.backend.simulation.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.baseballorders.backend.simulation.application.WaitingResultRegistry;
import com.example.baseballorders.messaging.SimulationPlayerMessage;
import com.example.baseballorders.messaging.SimulationRequestMessage;
import com.example.baseballorders.messaging.SimulationResultMessage;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;

@EnabledIfEnvironmentVariable(named = "SQS_ENDPOINT", matches = ".+")
@SpringBootTest
class SqsMessagingIntegrationTest {

    private static final String REQUEST_QUEUE = "simulation-request";
    private static final String RESULT_QUEUE = "simulation-result";

    @Autowired private SqsSimulatorMessagePublisher publisher;
    @Autowired private WaitingResultRegistry registry;
    @Autowired private SqsTemplate sqsTemplate;

    @DynamicPropertySource
    static void configureSqs(DynamicPropertyRegistry registry) {
        createQueues();
        registry.add("spring.cloud.aws.sqs.endpoint", () -> endpoint().toString());
        registry.add("spring.cloud.aws.credentials.access-key", () -> "test");
        registry.add("spring.cloud.aws.credentials.secret-key", () -> "test");
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:sqs-messaging");
    }

    @Test
    @DisplayName("要求を送信するとElasticMQのsimulation-requestキューから同じ内容を受信できる")
    void sendsRequestThroughElasticMq() {
        // given
        UUID simulationId = UUID.randomUUID();
        var request =
                new SimulationRequestMessage(
                        simulationId,
                        "1",
                        List.of(new SimulationPlayerMessage("選手1", 0.321f, 0.456f)));

        // when
        publisher.publish(request);
        List<Message> messages;
        try (var client = sqsClient()) {
            String queueUrl = queueUrl(client, REQUEST_QUEUE);
            messages =
                    client.receiveMessage(
                                    requestBuilder ->
                                            requestBuilder
                                                    .queueUrl(queueUrl)
                                                    .waitTimeSeconds(5)
                                                    .maxNumberOfMessages(1))
                            .messages();
        }

        // then
        assertAll(
                () -> assertEquals(1, messages.size()),
                () -> assertTrue(messages.getFirst().body().contains(simulationId.toString())),
                () -> assertTrue(messages.getFirst().body().contains("選手1")));
    }

    @Test
    @DisplayName("ElasticMQのsimulation-resultキューへ結果を送信すると待機中の要求が完了する")
    void receivesResultThroughElasticMq() throws Exception {
        // given
        UUID simulationId = UUID.randomUUID();
        var waiting = registry.register(simulationId);
        var message = new SimulationResultMessage(simulationId, "1", 5, 4);

        // when
        sqsTemplate.send(RESULT_QUEUE, message);
        var result = waiting.get(10, TimeUnit.SECONDS);

        // then
        assertAll(
                () -> assertEquals(simulationId, result.simulationId()),
                () -> assertEquals(5, result.score()),
                () -> assertEquals(4, result.runs()),
                () -> assertFalse(waiting.isCompletedExceptionally()));
    }

    private static SqsClient sqsClient() {
        return SqsClient.builder()
                .endpointOverride(endpoint())
                .region(Region.AP_NORTHEAST_1)
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create("test", "test")))
                .build();
    }

    private static URI endpoint() {
        return URI.create(System.getenv("SQS_ENDPOINT"));
    }

    private static void createQueues() {
        try (var client = sqsClient()) {
            String requestQueueUrl =
                    client.createQueue(request -> request.queueName(REQUEST_QUEUE)).queueUrl();
            String resultQueueUrl =
                    client.createQueue(request -> request.queueName(RESULT_QUEUE)).queueUrl();
            client.purgeQueue(request -> request.queueUrl(requestQueueUrl));
            client.purgeQueue(request -> request.queueUrl(resultQueueUrl));
        }
    }

    private static String queueUrl(SqsClient client, String queueName) {
        return client.getQueueUrl(request -> request.queueName(queueName)).queueUrl();
    }
}

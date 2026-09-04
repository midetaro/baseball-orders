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
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class SqsMessagingIntegrationTest {

    private static final String REQUEST_QUEUE = "simulation-request";
    private static final String RESULT_QUEUE = "simulation-result";

    @Container
    static final GenericContainer<?> ELASTIC_MQ =
            new GenericContainer<>(DockerImageName.parse("softwaremill/elasticmq-native:1.6.16"))
                    .withExposedPorts(9324)
                    .withCopyFileToContainer(
                            MountableFile.forClasspathResource("elasticmq.conf"),
                            "/opt/elasticmq.conf")
                    .withStartupTimeout(Duration.ofSeconds(30));

    @Autowired private SqsSimulatorMessagePublisher publisher;
    @Autowired private WaitingResultRegistry registry;
    @Autowired private SqsTemplate sqsTemplate;

    @DynamicPropertySource
    static void configureSqs(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.aws.sqs.endpoint", SqsMessagingIntegrationTest::endpoint);
        registry.add("spring.cloud.aws.credentials.access-key", () -> "test");
        registry.add("spring.cloud.aws.credentials.secret-key", () -> "test");
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
        return URI.create("http://" + ELASTIC_MQ.getHost() + ":" + ELASTIC_MQ.getMappedPort(9324));
    }

    private static String queueUrl(SqsClient client, String queueName) {
        return client.getQueueUrl(request -> request.queueName(queueName)).queueUrl();
    }
}

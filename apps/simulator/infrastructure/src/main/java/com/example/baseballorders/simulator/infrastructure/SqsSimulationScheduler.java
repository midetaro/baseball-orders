package com.example.baseballorders.simulator.infrastructure;

import com.example.baseballorders.simulator.application.LineUpMapper;
import com.example.baseballorders.simulator.application.SimulateGameUseCase;
import com.example.baseballorders.simulator.application.SimulationRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

/** Polls SQS for simulation requests and invokes the game simulation use case. */
@Component
public class SqsSimulationScheduler {

    private static final int MAX_MESSAGES_PER_POLL = 10;
    private static final int LONG_POLL_SECONDS = 10;

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final SimulateGameUseCase simulateGameUseCase;
    private final LineUpMapper lineUpMapper;
    private final String requestQueueUrl;

    /**
     * Creates an SQS simulation scheduler.
     *
     * @param sqsClient client used to receive and delete SQS messages
     * @param objectMapper mapper used to deserialize message bodies
     * @param simulateGameUseCase game simulation use case
     * @param lineUpMapper application mapper from request data to the domain lineup
     * @param requestQueueUrl URL of the simulation request queue
     */
    public SqsSimulationScheduler(
            SqsClient sqsClient,
            ObjectMapper objectMapper,
            SimulateGameUseCase simulateGameUseCase,
            LineUpMapper lineUpMapper,
            @Value("${simulation.sqs.request-queue-url}") String requestQueueUrl) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.simulateGameUseCase = simulateGameUseCase;
        this.lineUpMapper = lineUpMapper;
        this.requestQueueUrl = requestQueueUrl;
    }

    /** Receives pending requests from SQS once per minute and simulates each valid request. */
    @Scheduled(fixedDelay = 60_000L)
    public void poll() {
        var response =
                sqsClient.receiveMessage(
                        ReceiveMessageRequest.builder()
                                .queueUrl(requestQueueUrl)
                                .waitTimeSeconds(LONG_POLL_SECONDS)
                                .maxNumberOfMessages(MAX_MESSAGES_PER_POLL)
                                .build());

        response.messages()
                .forEach(
                        message -> {
                            SimulationRequest request = deserialize(message.body());
                            simulateGameUseCase.simulateGame(lineUpMapper.map(request.players()));
                            sqsClient.deleteMessage(
                                    DeleteMessageRequest.builder()
                                            .queueUrl(requestQueueUrl)
                                            .receiptHandle(message.receiptHandle())
                                            .build());
                        });
    }

    private SimulationRequest deserialize(String body) {
        try {
            return objectMapper.readValue(body, SimulationRequest.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(
                    "Failed to deserialize an SQS simulation request", exception);
        }
    }
}

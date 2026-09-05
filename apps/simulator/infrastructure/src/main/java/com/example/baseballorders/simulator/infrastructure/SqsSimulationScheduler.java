package com.example.baseballorders.simulator.infrastructure;

import com.example.baseballorders.messaging.SimulationRequestMessage;
import com.example.baseballorders.messaging.SimulationResultMessage;
import com.example.baseballorders.simulator.application.LineUpMapper;
import com.example.baseballorders.simulator.application.contract.SimulationResponse;
import com.example.baseballorders.simulator.application.usecase.SimulateGameUseCase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

/** Polls SQS for simulation requests and invokes the game simulation use case. */
@Component
public class SqsSimulationScheduler {

    private static final int MAX_MESSAGES_PER_POLL = 10;
    private static final int LONG_POLL_SECONDS = 10;

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final SimulateGameUseCase simulateGameUseCase;
    private final LineUpMapper lineUpMapper;
    private final String requestQueueName;
    private final String resultQueueName;

    /**
     * Creates an SQS simulation scheduler.
     *
     * @param sqsClient client used to receive and delete SQS messages
     * @param objectMapper mapper used to deserialize message bodies
     * @param simulateGameUseCase game simulation use case
     * @param lineUpMapper application mapper from request data to the domain lineup
     * @param requestQueueName name of the simulation request queue
     * @param resultQueueName name of the simulation result queue
     */
    public SqsSimulationScheduler(
            SqsClient sqsClient,
            ObjectMapper objectMapper,
            SimulateGameUseCase simulateGameUseCase,
            LineUpMapper lineUpMapper,
            @Value("${simulation.sqs.request-queue-name}") String requestQueueName,
            @Value("${simulation.sqs.result-queue-name}") String resultQueueName) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.simulateGameUseCase = simulateGameUseCase;
        this.lineUpMapper = lineUpMapper;
        this.requestQueueName = requestQueueName;
        this.resultQueueName = resultQueueName;
    }

    /**
     * Receives pending requests from SQS, sends each simulation response to the configured result
     * queue, and deletes the source message after all results are sent successfully.
     */
    @Scheduled(fixedDelayString = "${simulation.sqs.poll-fixed-delay}")
    public void poll() {
        String requestQueueUrl = queueUrl(requestQueueName);
        String resultQueueUrl = queueUrl(resultQueueName);
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
                            SimulationRequestMessage request = deserialize(message.body());
                            List<SimulationResponse> results =
                                    simulateGameUseCase.invoke(lineUpMapper.map(request.players()));
                            results.forEach(
                                    result ->
                                            sqsClient.sendMessage(
                                                    SendMessageRequest.builder()
                                                            .queueUrl(resultQueueUrl)
                                                            .messageBody(
                                                                    serialize(
                                                                            new SimulationResultMessage(
                                                                                    request
                                                                                            .simulationId(),
                                                                                    request
                                                                                            .version(),
                                                                                    result.score(),
                                                                                    result.runs())))
                                                            .build()));
                            sqsClient.deleteMessage(
                                    DeleteMessageRequest.builder()
                                            .queueUrl(requestQueueUrl)
                                            .receiptHandle(message.receiptHandle())
                                            .build());
                        });
    }

    private SimulationRequestMessage deserialize(String body) {
        try {
            return objectMapper.readValue(body, SimulationRequestMessage.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(
                    "Failed to deserialize an SQS simulation request", exception);
        }
    }

    private String serialize(SimulationResultMessage response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(
                    "Failed to serialize an SQS simulation response", exception);
        }
    }

    private String queueUrl(String queueName) {
        return sqsClient
                .getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build())
                .queueUrl();
    }
}

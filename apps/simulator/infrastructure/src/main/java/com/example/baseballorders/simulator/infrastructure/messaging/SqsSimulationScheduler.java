package com.example.baseballorders.simulator.infrastructure.messaging;

import com.example.baseballorders.messaging.SimulationRequestMessage;
import com.example.baseballorders.messaging.SimulationResultMessage;
import com.example.baseballorders.simulator.application.contract.SimulationResult;
import com.example.baseballorders.simulator.application.usecase.SimulateGameUseCase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class SqsSimulationScheduler {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final SimulateGameUseCase simulateGameUseCase;
    private final LineUpMapper lineUpMapper;
    private final String requestQueueName;
    private final String resultQueueName;
    private final int maxMessagesPerPoll;
    private final int longPollSeconds;

    /**
     * Creates an SQS simulation scheduler.
     *
     * @param sqsClient client used to receive and delete SQS messages
     * @param objectMapper mapper used to deserialize message bodies
     * @param simulateGameUseCase game simulation use case
     * @param lineUpMapper application mapper from request data to the domain lineup
     * @param requestQueueName name of the simulation request queue
     * @param resultQueueName name of the simulation result queue
     * @param maxMessagesPerPoll maximum requests received in one poll
     * @param longPollSeconds maximum seconds spent waiting for a request
     */
    public SqsSimulationScheduler(
            SqsClient sqsClient,
            ObjectMapper objectMapper,
            SimulateGameUseCase simulateGameUseCase,
            LineUpMapper lineUpMapper,
            @Value("${simulation.sqs.request-queue-name}") String requestQueueName,
            @Value("${simulation.sqs.result-queue-name}") String resultQueueName,
            @Value("${simulation.sqs.max-messages-per-poll}") int maxMessagesPerPoll,
            @Value("${simulation.sqs.long-poll-seconds}") int longPollSeconds) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.simulateGameUseCase = simulateGameUseCase;
        this.lineUpMapper = lineUpMapper;
        this.requestQueueName = requestQueueName;
        this.resultQueueName = resultQueueName;
        this.maxMessagesPerPoll = maxMessagesPerPoll;
        this.longPollSeconds = longPollSeconds;
    }

    /**
     * Receives pending requests from SQS, sends all simulation results in one message to the
     * configured result queue, and deletes the source message only after that send succeeds.
     * Rejects missing request fields before mapping or simulation, leaving invalid messages on SQS.
     * A failure processing one message is logged without interrupting later messages in the same
     * poll.
     */
    @Scheduled(fixedDelayString = "${simulation.sqs.poll-fixed-delay}")
    public void poll() {
        String requestQueueUrl = queueUrl(requestQueueName);
        String resultQueueUrl = queueUrl(resultQueueName);
        // 受信
        var response =
                sqsClient.receiveMessage(
                        ReceiveMessageRequest.builder()
                                .queueUrl(requestQueueUrl)
                                .waitTimeSeconds(longPollSeconds)
                                .maxNumberOfMessages(maxMessagesPerPoll)
                                .build());
        for (var message : response.messages()) {
            try {
                // 送信
                SimulationRequestMessage request = deserialize(message.body());
                SimulationResult simulationResult =
                        simulateGameUseCase.invoke(lineUpMapper.map(request.players()));
                var resultMessage =
                        new SimulationResultMessage(
                                request.simulationId(),
                                SimulationResultMessage.CURRENT_VERSION,
                                new SimulationResultMessage.GameScoreStatistics(
                                        simulationResult.statistics().averageScore(),
                                        simulationResult.statistics().medianScore(),
                                        simulationResult.statistics().maximumScore(),
                                        simulationResult.statistics().gameCount(),
                                        simulationResult.statistics().scoreDistribution()),
                                new SimulationResultMessage.GameContentStatistics(
                                        simulationResult.statistics().homeRunCount(),
                                        simulationResult.statistics().soloHomeRunCount(),
                                        simulationResult.statistics().twoRunHomeRunCount(),
                                        simulationResult.statistics().threeRunHomeRunCount(),
                                        simulationResult.statistics().grandSlamCount(),
                                        simulationResult.statistics().buntCount(),
                                        simulationResult.statistics().stealCount(),
                                        simulationResult.statistics().buntFailureCount(),
                                        simulationResult.statistics().stealFailureCount(),
                                        simulationResult.statistics().advancingBuntCount(),
                                        simulationResult.statistics().squeezeBuntCount(),
                                        simulationResult.statistics().advancingBuntFailureCount(),
                                        simulationResult.statistics().squeezeBuntFailureCount(),
                                        simulationResult.statistics().stealToSecondCount(),
                                        simulationResult.statistics().stealToThirdCount()));
                sqsClient.sendMessage(
                        SendMessageRequest.builder()
                                .queueUrl(resultQueueUrl)
                                .messageBody(serialize(resultMessage))
                                .build());
                sqsClient.deleteMessage(
                        DeleteMessageRequest.builder()
                                .queueUrl(requestQueueUrl)
                                .receiptHandle(message.receiptHandle())
                                .build());
            } catch (Throwable throwable) {
                log.error(
                        "Failed to process SQS simulation request messageId={}",
                        message.messageId(),
                        throwable);
            }
        }
    }

    private SimulationRequestMessage deserialize(String body) {
        try {
            return Objects.requireNonNull(
                    objectMapper.readValue(body, SimulationRequestMessage.class),
                    "request must not be null");
        } catch (JsonProcessingException | NullPointerException exception) {
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

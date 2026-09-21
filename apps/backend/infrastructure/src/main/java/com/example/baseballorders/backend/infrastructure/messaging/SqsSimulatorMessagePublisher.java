package com.example.baseballorders.backend.infrastructure.messaging;

import com.example.baseballorders.backend.application.adapter.SimulatorMessagePublisher;
import com.example.baseballorders.backend.application.dto.SimulationRequest;
import com.example.baseballorders.messaging.SimulationPlayerMessage;
import com.example.baseballorders.messaging.SimulationRequestMessage;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** SqsTemplateを使用してsimulation-requestキューへ要求を送信する。 */
@Component
public final class SqsSimulatorMessagePublisher implements SimulatorMessagePublisher {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(SqsSimulatorMessagePublisher.class);

    private final SqsTemplate sqsTemplate;
    private final String requestQueueName;

    /**
     * Creates a publisher for the configured request queue.
     *
     * @param sqsTemplate template used to send SQS messages
     * @param requestQueueName name of the simulation request queue
     */
    public SqsSimulatorMessagePublisher(
            SqsTemplate sqsTemplate,
            @Value("${simulation.sqs.request-queue-name}") String requestQueueName) {
        this.sqsTemplate = sqsTemplate;
        this.requestQueueName = requestQueueName;
    }

    @Override
    public void publish(SimulationRequest request) {
        var message =
                new SimulationRequestMessage(
                        request.simulationId(),
                        request.version(),
                        request.players().stream()
                                .map(
                                        player ->
                                                new SimulationPlayerMessage(
                                                        player.name(),
                                                        player.hitAverage(),
                                                        player.sluggish(),
                                                        player.buntSuccessRate(),
                                                        player.buntEnabled(),
                                                        player.stealSuccessRate(),
                                                        player.stealEnabled(),
                                                        toMessagePersonality(player.personality())))
                                .toList());
        sqsTemplate.send(requestQueueName, message);
        LOGGER.info("simulation request sent simulationId={}", request.simulationId());
    }

    /** 共有メッセージを送信する。既存のアダプタ利用者との互換性を保つ。 */
    public void publish(SimulationRequestMessage request) {
        sqsTemplate.send(requestQueueName, request);
        LOGGER.info("simulation request sent simulationId={}", request.simulationId());
    }

    private static com.example.baseballorders.messaging.PlayerPersonality toMessagePersonality(
            com.example.baseballorders.backend.domain.PlayerPersonality personality) {
        return switch (personality) {
            case DEFAULT -> com.example.baseballorders.messaging.PlayerPersonality.DEFAULT;
            case EAGER_SLUGGISH ->
                    com.example.baseballorders.messaging.PlayerPersonality.EAGER_SLUGGISH;
            case EAGER_STEAL -> com.example.baseballorders.messaging.PlayerPersonality.EAGER_STEAL;
            case EAGER_BUNT -> com.example.baseballorders.messaging.PlayerPersonality.EAGER_BUNT;
        };
    }
}

package com.example.baseballorders.backend.simulation.infrastructure.messaging;

import com.example.baseballorders.backend.simulation.application.SimulationRequest;
import com.example.baseballorders.backend.simulation.application.SimulatorMessagePublisher;
import com.example.baseballorders.messaging.SimulationPlayerMessage;
import com.example.baseballorders.messaging.SimulationRequestMessage;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** SqsTemplateを使用してsimulation-requestキューへ要求を送信する。 */
@Component
@RequiredArgsConstructor
public final class SqsSimulatorMessagePublisher implements SimulatorMessagePublisher {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(SqsSimulatorMessagePublisher.class);

    private final SqsTemplate sqsTemplate;

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
                                                        player.sluggish()))
                                .toList());
        sqsTemplate.send("simulation-request", message);
        LOGGER.info("simulation request sent simulationId={}", request.simulationId());
    }

    /** 共有メッセージを送信する。既存のアダプタ利用者との互換性を保つ。 */
    public void publish(SimulationRequestMessage request) {
        sqsTemplate.send("simulation-request", request);
        LOGGER.info("simulation request sent simulationId={}", request.simulationId());
    }
}

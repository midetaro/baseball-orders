package com.example.baseballorders.backend.simulation.infrastructure.messaging;

import com.example.baseballorders.messaging.SimulationRequestMessage;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** SqsTemplateを使用してsimulation-requestキューへ要求を送信する。 */
@Component
public final class SqsSimulatorMessagePublisher implements SimulatorMessagePublisher {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(SqsSimulatorMessagePublisher.class);

    private final SqsTemplate sqsTemplate;

    /**
     * SQS送信に使用するtemplateを指定してPublisherを作成する。
     *
     * @param sqsTemplate Spring Cloud AWSのSQS template
     */
    public SqsSimulatorMessagePublisher(SqsTemplate sqsTemplate) {
        this.sqsTemplate = sqsTemplate;
    }

    @Override
    public void publish(SimulationRequestMessage request) {
        sqsTemplate.send("simulation-request", request);
        LOGGER.info("simulation request sent simulationId={}", request.simulationId());
    }
}

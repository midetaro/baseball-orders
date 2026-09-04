package com.example.baseballorders.backend.simulation.infrastructure.messaging;

import com.example.baseballorders.backend.simulation.application.WaitingResultRegistry;
import com.example.baseballorders.backend.simulation.domain.SimulationResult;
import com.example.baseballorders.messaging.SimulationResultMessage;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** simulation-result SQSから結果を受け取りHTTP待機要求を完了する。 */
@Component
@RequiredArgsConstructor
public final class SimulationResultListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimulationResultListener.class);

    private final WaitingResultRegistry registry;

    /**
     * SQS結果をbackend結果へ変換し、同じsimulation IDの待機を完了する。
     *
     * @param message simulatorから受信した結果メッセージ
     */
    @SqsListener("simulation-result")
    public void receive(SimulationResultMessage message) {
        LOGGER.info("simulation result received simulationId={}", message.simulationId());
        boolean completed =
                registry.complete(
                        message.simulationId(),
                        new SimulationResult(
                                message.simulationId(), message.score(), message.runs()));
        if (!completed) {
            LOGGER.warn("simulation result ignored simulationId={}", message.simulationId());
        }
    }
}

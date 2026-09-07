package com.example.baseballorders.backend.infrastructure.messaging;

import com.example.baseballorders.backend.application.WaitingResultRegistry;
import com.example.baseballorders.backend.domain.SimulationResult;
import com.example.baseballorders.messaging.SimulationResultMessage;
import io.awspring.cloud.sqs.annotation.SqsListener;
import java.util.Objects;
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
    @SqsListener("${simulation.sqs.result-queue-name}")
    public void receive(SimulationResultMessage message) {
        LOGGER.info("simulation result received simulationId={}", message.simulationId());
        var statistics =
                Objects.requireNonNull(
                        message.statistics(), "simulation result statistics must not be null");
        LOGGER.info("試合数={}", message.results().size());
        boolean completed =
                registry.complete(
                        message.simulationId(),
                        new SimulationResult(
                                message.simulationId(),
                                message.results().stream()
                                        .map(
                                                result ->
                                                        new SimulationResult.Result(
                                                                result.score(), result.runs()))
                                        .toList(),
                                new SimulationResult.Statistics(
                                        statistics.averageScore(),
                                        statistics.medianScore(),
                                        statistics.maximumScore())));
        if (!completed) {
            LOGGER.warn("simulation result ignored simulationId={}", message.simulationId());
        }
    }
}

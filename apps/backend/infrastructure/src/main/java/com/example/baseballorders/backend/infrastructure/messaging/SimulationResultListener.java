package com.example.baseballorders.backend.infrastructure.messaging;

import com.example.baseballorders.backend.application.SimulationResultService;
import com.example.baseballorders.backend.application.WaitingResultRegistry;
import com.example.baseballorders.backend.domain.SimulationResult;
import com.example.baseballorders.messaging.SimulationResultMessage;
import io.awspring.cloud.sqs.annotation.SqsListener;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** simulation-result SQSから結果を受け取りHTTP待機要求を完了する。 */
@Component
public class SimulationResultListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimulationResultListener.class);

    private final WaitingResultRegistry registry;
    private final SimulationResultService resultService;

    /** テスト用に永続化を行わない結果Listenerを作成する。 */
    public SimulationResultListener(WaitingResultRegistry registry) {
        this.registry = registry;
        this.resultService = null;
    }

    /** Spring用の結果Listenerを作成する。 */
    @Autowired
    public SimulationResultListener(
            WaitingResultRegistry registry, SimulationResultService resultService) {
        this.registry = registry;
        this.resultService = resultService;
    }

    /**
     * SQS結果をbackend結果へ変換し、同じsimulation IDの待機を完了する。
     *
     * @param message simulatorから受信した結果メッセージ
     */
    @SqsListener("${simulation.sqs.result-queue-name}")
    @Transactional
    public void receive(SimulationResultMessage message) {
        LOGGER.info("simulation result received simulationId={}", message.simulationId());
        var statistics =
                Objects.requireNonNull(
                        message.statistics(), "simulation result statistics must not be null");
        LOGGER.info("試合数={}", message.results().size());
        var result =
                new SimulationResult(
                        message.simulationId(),
                        message.results().stream()
                                .map(
                                        messageResult ->
                                                new SimulationResult.Result(
                                                        messageResult.score(),
                                                        messageResult.runs()))
                                .toList(),
                        new SimulationResult.Statistics(
                                statistics.averageScore(),
                                statistics.medianScore(),
                                statistics.maximumScore()));
        if (resultService != null)
            resultService.saveSimulationResult(message.simulationId(), result);
        boolean completed = registry.complete(message.simulationId(), result);
        if (!completed) {
            LOGGER.warn("simulation result ignored simulationId={}", message.simulationId());
        }
    }
}

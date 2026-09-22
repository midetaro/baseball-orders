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
        var scoreStatistics =
                Objects.requireNonNull(
                        message.gameScoreStatistics(),
                        "simulation result statistics must not be null");
        var contentStatistics =
                Objects.requireNonNull(
                        message.gameContentStatistics(),
                        "simulation result statistics must not be null");
        LOGGER.info("試合数={}", scoreStatistics.gameCount());
        boolean completed =
                registry.complete(
                        message.simulationId(),
                        new SimulationResult(
                                message.simulationId(),
                                new SimulationResult.Statistics(
                                        scoreStatistics.averageScore(),
                                        scoreStatistics.medianScore(),
                                        scoreStatistics.maximumScore(),
                                        scoreStatistics.gameCount(),
                                        scoreStatistics.scoreDistribution(),
                                        contentStatistics.homeRunCount(),
                                        contentStatistics.soloHomeRunCount(),
                                        contentStatistics.twoRunHomeRunCount(),
                                        contentStatistics.threeRunHomeRunCount(),
                                        contentStatistics.grandSlamCount(),
                                        contentStatistics.buntCount(),
                                        contentStatistics.stealCount(),
                                        contentStatistics.buntFailureCount(),
                                        contentStatistics.stealFailureCount(),
                                        contentStatistics.advancingBuntCount(),
                                        contentStatistics.squeezeBuntCount(),
                                        contentStatistics.advancingBuntFailureCount(),
                                        contentStatistics.squeezeBuntFailureCount(),
                                        contentStatistics.stealToSecondCount(),
                                        contentStatistics.stealToThirdCount())));
        if (!completed) {
            LOGGER.warn("simulation result ignored simulationId={}", message.simulationId());
        }
    }
}

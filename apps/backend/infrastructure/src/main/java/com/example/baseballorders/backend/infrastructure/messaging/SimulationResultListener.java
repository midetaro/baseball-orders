package com.example.baseballorders.backend.infrastructure.messaging;

import com.example.baseballorders.backend.application.WaitingResultRegistry;
import com.example.baseballorders.backend.domain.GameTransitionBuilder;
import com.example.baseballorders.backend.domain.SimulationResultBuilder;
import com.example.baseballorders.backend.domain.StatisticsBuilder;
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

    private static com.example.baseballorders.backend.domain.GameTransition toDomainTransition(
            com.example.baseballorders.messaging.GameTransitionMessage transition) {
        return GameTransitionBuilder.gameTransition()
                .inning(transition.inning())
                .actionResult(transition.actionResult())
                .outCount(transition.outCount())
                .cumulativeScore(transition.cumulativeScore())
                .runnerState(transition.runnerState())
                .build();
    }

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
                        SimulationResultBuilder.simulationResult()
                                .simulationId(message.simulationId())
                                .statistics(
                                        StatisticsBuilder.statistics()
                                                .averageScore(scoreStatistics.averageScore())
                                                .medianScore(scoreStatistics.medianScore())
                                                .maximumScore(scoreStatistics.maximumScore())
                                                .gameCount(scoreStatistics.gameCount())
                                                .scoreDistribution(
                                                        scoreStatistics.scoreDistribution())
                                                .hitCount(contentStatistics.hitCount())
                                                .singleHitCount(contentStatistics.singleHitCount())
                                                .doubleHitCount(contentStatistics.doubleHitCount())
                                                .tripleHitCount(contentStatistics.tripleHitCount())
                                                .homeRunCount(contentStatistics.homeRunCount())
                                                .soloHomeRunCount(
                                                        contentStatistics.soloHomeRunCount())
                                                .twoRunHomeRunCount(
                                                        contentStatistics.twoRunHomeRunCount())
                                                .threeRunHomeRunCount(
                                                        contentStatistics.threeRunHomeRunCount())
                                                .grandSlamCount(contentStatistics.grandSlamCount())
                                                .buntCount(contentStatistics.buntCount())
                                                .stealCount(contentStatistics.stealCount())
                                                .buntFailureCount(
                                                        contentStatistics.buntFailureCount())
                                                .stealFailureCount(
                                                        contentStatistics.stealFailureCount())
                                                .advancingBuntCount(
                                                        contentStatistics.advancingBuntCount())
                                                .squeezeBuntCount(
                                                        contentStatistics.squeezeBuntCount())
                                                .advancingBuntFailureCount(
                                                        contentStatistics
                                                                .advancingBuntFailureCount())
                                                .squeezeBuntFailureCount(
                                                        contentStatistics.squeezeBuntFailureCount())
                                                .stealToSecondCount(
                                                        contentStatistics.stealToSecondCount())
                                                .stealToThirdCount(
                                                        contentStatistics.stealToThirdCount())
                                                .build())
                                .transitions(
                                        message.gameTransitions().stream()
                                                .map(SimulationResultListener::toDomainTransition)
                                                .toList())
                                .build());
        if (!completed) {
            LOGGER.warn("simulation result ignored simulationId={}", message.simulationId());
        }
    }
}

package com.example.baseballorders.backend.infrastructure.messaging;

import com.example.baseballorders.backend.application.adapter.SimulatorClient;
import com.example.baseballorders.backend.application.dto.SimulationRequest;
import com.example.baseballorders.backend.application.exception.SimulationSendException;
import com.example.baseballorders.backend.application.exception.SimulationTimeoutException;
import com.example.baseballorders.backend.domain.GameTransition;
import com.example.baseballorders.backend.domain.GameTransitionBuilder;
import com.example.baseballorders.backend.domain.PlayerData;
import com.example.baseballorders.backend.domain.SimulationResult;
import com.example.baseballorders.backend.domain.SimulationResultBuilder;
import com.example.baseballorders.backend.domain.StatisticsBuilder;
import com.example.baseballorders.messaging.GameContentStatistics;
import com.example.baseballorders.messaging.GameScoreStatistics;
import com.example.baseballorders.messaging.GameTransitionMessage;
import com.example.baseballorders.messaging.PitcherPersonality;
import com.example.baseballorders.messaging.SimulationPlayerMessage;
import com.example.baseballorders.messaging.SimulationRequestMessage;
import com.example.baseballorders.messaging.SimulationResultMessage;
import com.example.baseballorders.messaging.SimulationServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** simulatorのgRPCサービスをunary RPCで同期的に呼び出し、結果をbackendのシミュレーション結果へ変換する。 */
@Slf4j
@Component
public final class GrpcSimulatorClient implements SimulatorClient {

    private final SimulationServiceGrpc.SimulationServiceBlockingStub stub;
    private final Duration resultTimeout;

    /**
     * simulatorへのチャネルと応答待機時間を指定してClientを作成する。
     *
     * @param channel simulatorのgRPCサーバーへ接続するチャネル
     * @param resultTimeout simulatorの応答を待機する時間。超過するとRPCを打ち切る
     * @throws NullPointerException channelまたはresultTimeoutがnullの場合
     */
    public GrpcSimulatorClient(
            ManagedChannel channel,
            @Value("${baseball-orders.simulation.result-timeout}") Duration resultTimeout) {
        this.stub = SimulationServiceGrpc.newBlockingStub(Objects.requireNonNull(channel));
        this.resultTimeout =
                Objects.requireNonNull(resultTimeout, "resultTimeout must not be null");
    }

    /**
     * {@inheritDoc}
     *
     * <p>RPCの期限には設定された応答待機時間を使用し、期限超過は{@link SimulationTimeoutException}、それ以外のgRPCエラーと
     * 統計を含まない応答は{@link SimulationSendException}へ変換する。
     */
    @Override
    public SimulationResult simulate(SimulationRequest request) {
        UUID simulationId = request.simulationId();
        SimulationResultMessage response;
        try {
            response =
                    stub.withDeadlineAfter(resultTimeout.toMillis(), TimeUnit.MILLISECONDS)
                            .simulate(toRequestMessage(request));
        } catch (StatusRuntimeException exception) {
            if (exception.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
                throw new SimulationTimeoutException(simulationId);
            }
            throw new SimulationSendException(simulationId, exception);
        }
        log.info("simulation result received simulationId={}", response.getSimulationId());
        if (!response.hasGameScoreStatistics() || !response.hasGameContentStatistics()) {
            throw new SimulationSendException(
                    simulationId,
                    new IllegalStateException("simulation result statistics must not be absent"));
        }
        return toDomainResult(simulationId, response);
    }

    private static SimulationRequestMessage toRequestMessage(SimulationRequest request) {
        var builder =
                SimulationRequestMessage.newBuilder()
                        .setSimulationId(request.simulationId().toString())
                        .setPitcherPersonality(PitcherPersonality.PITCHER_PERSONALITY_DEFAULT)
                        .setMode(toMessageMode(request.mode()));
        for (PlayerData player : request.players()) {
            builder.addPlayers(toPlayerMessage(player));
        }
        return builder.build();
    }

    private static SimulationPlayerMessage toPlayerMessage(PlayerData player) {
        return SimulationPlayerMessage.newBuilder()
                .setName(player.name())
                .setHitAverage(player.hitAverage())
                .setBuntEnabled(player.buntEnabled())
                .setStealEnabled(player.stealEnabled())
                .setPersonality(toMessagePersonality(player.personality()))
                .build();
    }

    private static com.example.baseballorders.messaging.PlayerPersonality toMessagePersonality(
            com.example.baseballorders.backend.domain.PlayerPersonality personality) {
        return switch (personality) {
            case DEFAULT ->
                    com.example.baseballorders.messaging.PlayerPersonality
                            .PLAYER_PERSONALITY_DEFAULT;
            case MIDDLE_DISTANCE ->
                    com.example.baseballorders.messaging.PlayerPersonality
                            .PLAYER_PERSONALITY_MIDDLE_DISTANCE;
            case EAGER_SLUGGISH ->
                    com.example.baseballorders.messaging.PlayerPersonality
                            .PLAYER_PERSONALITY_EAGER_SLUGGISH;
            case HIGH_ON_BASE ->
                    com.example.baseballorders.messaging.PlayerPersonality
                            .PLAYER_PERSONALITY_HIGH_ON_BASE;
            case EAGER_STEAL ->
                    com.example.baseballorders.messaging.PlayerPersonality
                            .PLAYER_PERSONALITY_EAGER_STEAL;
            case EAGER_BUNT ->
                    com.example.baseballorders.messaging.PlayerPersonality
                            .PLAYER_PERSONALITY_EAGER_BUNT;
        };
    }

    private static com.example.baseballorders.messaging.SimulationMode toMessageMode(
            com.example.baseballorders.backend.domain.SimulationMode mode) {
        return switch (mode) {
            case LARGE_SCALE_RUN ->
                    com.example.baseballorders.messaging.SimulationMode
                            .SIMULATION_MODE_LARGE_SCALE_RUN;
            case SINGLE_GAME_RUN ->
                    com.example.baseballorders.messaging.SimulationMode
                            .SIMULATION_MODE_SINGLE_GAME_RUN;
        };
    }

    private static SimulationResult toDomainResult(
            UUID simulationId, SimulationResultMessage message) {
        GameScoreStatistics score = message.getGameScoreStatistics();
        GameContentStatistics content = message.getGameContentStatistics();
        return SimulationResultBuilder.simulationResult()
                .simulationId(simulationId)
                .statistics(
                        StatisticsBuilder.statistics()
                                .averageScore(score.getAverageScore())
                                .medianScore(score.getMedianScore())
                                .maximumScore(score.getMaximumScore())
                                .gameCount(score.getGameCount())
                                .scoreDistribution(score.getScoreDistributionMap())
                                .hitCount(content.getHitCount())
                                .singleHitCount(content.getSingleHitCount())
                                .doubleHitCount(content.getDoubleHitCount())
                                .tripleHitCount(content.getTripleHitCount())
                                .homeRunCount(content.getHomeRunCount())
                                .soloHomeRunCount(content.getSoloHomeRunCount())
                                .twoRunHomeRunCount(content.getTwoRunHomeRunCount())
                                .threeRunHomeRunCount(content.getThreeRunHomeRunCount())
                                .grandSlamCount(content.getGrandSlamCount())
                                .buntCount(content.getBuntCount())
                                .stealCount(content.getStealCount())
                                .buntFailureCount(content.getBuntFailureCount())
                                .stealFailureCount(content.getStealFailureCount())
                                .advancingBuntCount(content.getAdvancingBuntCount())
                                .squeezeBuntCount(content.getSqueezeBuntCount())
                                .advancingBuntFailureCount(content.getAdvancingBuntFailureCount())
                                .squeezeBuntFailureCount(content.getSqueezeBuntFailureCount())
                                .stealToSecondCount(content.getStealToSecondCount())
                                .stealToThirdCount(content.getStealToThirdCount())
                                .build())
                .transitions(
                        message.getGameTransitionsList().stream()
                                .map(GrpcSimulatorClient::toDomainTransition)
                                .toList())
                .build();
    }

    private static GameTransition toDomainTransition(GameTransitionMessage transition) {
        return GameTransitionBuilder.gameTransition()
                .inning(transition.getInning())
                .actionResult(transition.getActionResult())
                .outCount(transition.getOutCount())
                .cumulativeScore(transition.getCumulativeScore())
                .runnerState(transition.getRunnerState())
                .build();
    }
}

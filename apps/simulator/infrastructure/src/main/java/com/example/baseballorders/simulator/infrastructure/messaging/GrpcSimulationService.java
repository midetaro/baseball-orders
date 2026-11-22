package com.example.baseballorders.simulator.infrastructure.messaging;

import com.example.baseballorders.messaging.GameContentStatistics;
import com.example.baseballorders.messaging.GameScoreStatistics;
import com.example.baseballorders.messaging.GameTransitionMessage;
import com.example.baseballorders.messaging.SimulationMode;
import com.example.baseballorders.messaging.SimulationRequestMessage;
import com.example.baseballorders.messaging.SimulationResultMessage;
import com.example.baseballorders.messaging.SimulationServiceGrpc;
import com.example.baseballorders.simulator.application.contract.SimulationResult;
import com.example.baseballorders.simulator.application.usecase.SimulateGameUseCase;
import com.example.baseballorders.simulator.application.usecase.SimulationRunMode;
import com.example.baseballorders.simulator.domain.statistics.GameTransition;
import com.example.baseballorders.simulator.domain.statistics.ScoreStatistics;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** gRPCで受けたシミュレーション要求をユースケースへ委譲し、結果をメッセージとして返す。 */
@Component
@Slf4j
@RequiredArgsConstructor
public class GrpcSimulationService extends SimulationServiceGrpc.SimulationServiceImplBase {

    private final SimulateGameUseCase simulateGameUseCase;
    private final LineUpMapper lineUpMapper;

    /**
     * 要求の打順と実行モードでシミュレーションを実行し、全試合の統計を1つの結果として返す。
     *
     * <p>要求の内容が不正な場合（打順の人数や列挙値が不正など）は {@code INVALID_ARGUMENT}、 それ以外の予期しない失敗は {@code INTERNAL}
     * でRPCを失敗させる。どちらも要求のsimulation_idをログに残す。
     *
     * @param request 打順と実行モードを含むシミュレーション要求
     * @param responseObserver 結果または失敗を返す応答先
     */
    @Override
    public void simulate(
            SimulationRequestMessage request,
            StreamObserver<SimulationResultMessage> responseObserver) {
        SimulationResultMessage result;
        try {
            SimulationResult simulationResult =
                    simulateGameUseCase.invoke(
                            lineUpMapper.map(request.getPlayersList()),
                            toRunMode(request.getMode()));
            result = toResultMessage(request.getSimulationId(), simulationResult);
        } catch (IllegalArgumentException exception) {
            log.warn(
                    "Rejected simulation request simulationId={}",
                    request.getSimulationId(),
                    exception);
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription(exception.getMessage())
                            .withCause(exception)
                            .asRuntimeException());
            return;
        } catch (RuntimeException exception) {
            log.error("Failed to simulate simulationId={}", request.getSimulationId(), exception);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Simulation failed")
                            .withCause(exception)
                            .asRuntimeException());
            return;
        }
        responseObserver.onNext(result);
        responseObserver.onCompleted();
    }

    private static SimulationRunMode toRunMode(SimulationMode mode) {
        return switch (mode) {
            case SIMULATION_MODE_LARGE_SCALE_RUN -> SimulationRunMode.LARGE_SCALE_RUN;
            case SIMULATION_MODE_SINGLE_GAME_RUN -> SimulationRunMode.SINGLE_GAME_RUN;
            case UNRECOGNIZED -> throw new IllegalArgumentException("Unrecognized mode: " + mode);
        };
    }

    private static SimulationResultMessage toResultMessage(
            String simulationId, SimulationResult simulationResult) {
        ScoreStatistics statistics = simulationResult.statistics();
        SimulationResultMessage.Builder builder =
                SimulationResultMessage.newBuilder()
                        .setSimulationId(simulationId)
                        .setGameScoreStatistics(
                                GameScoreStatistics.newBuilder()
                                        .setAverageScore(statistics.averageScore())
                                        .setMedianScore(statistics.medianScore())
                                        .setMaximumScore(statistics.maximumScore())
                                        .setGameCount(statistics.gameCount())
                                        .putAllScoreDistribution(statistics.scoreDistribution()))
                        .setGameContentStatistics(
                                GameContentStatistics.newBuilder()
                                        .setHitCount(statistics.hitCount())
                                        .setSingleHitCount(statistics.singleHitCount())
                                        .setDoubleHitCount(statistics.doubleHitCount())
                                        .setTripleHitCount(statistics.tripleHitCount())
                                        .setHomeRunCount(statistics.homeRunCount())
                                        .setSoloHomeRunCount(statistics.soloHomeRunCount())
                                        .setTwoRunHomeRunCount(statistics.twoRunHomeRunCount())
                                        .setThreeRunHomeRunCount(statistics.threeRunHomeRunCount())
                                        .setGrandSlamCount(statistics.grandSlamCount())
                                        .setBuntCount(statistics.buntCount())
                                        .setStealCount(statistics.stealCount())
                                        .setBuntFailureCount(statistics.buntFailureCount())
                                        .setStealFailureCount(statistics.stealFailureCount())
                                        .setAdvancingBuntCount(statistics.advancingBuntCount())
                                        .setSqueezeBuntCount(statistics.squeezeBuntCount())
                                        .setAdvancingBuntFailureCount(
                                                statistics.advancingBuntFailureCount())
                                        .setSqueezeBuntFailureCount(
                                                statistics.squeezeBuntFailureCount())
                                        .setStealToSecondCount(statistics.stealToSecondCount())
                                        .setStealToThirdCount(statistics.stealToThirdCount()));
        for (GameTransition transition : simulationResult.transitions()) {
            builder.addGameTransitions(toMessageTransition(transition));
        }
        return builder.build();
    }

    private static GameTransitionMessage toMessageTransition(GameTransition transition) {
        return GameTransitionMessage.newBuilder()
                .setInning(transition.inning())
                .setActionResult(transition.actionResult())
                .setOutCount(transition.outCount())
                .setCumulativeScore(transition.cumulativeScore())
                .setRunnerState(transition.runnerState())
                .build();
    }
}

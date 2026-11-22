package com.example.baseballorders.simulator.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.baseballorders.messaging.SimulationMode;
import com.example.baseballorders.messaging.SimulationRequestMessage;
import com.example.baseballorders.messaging.SimulationServiceGrpc;
import com.example.baseballorders.simulator.application.contract.SimulationResultBuilder;
import com.example.baseballorders.simulator.application.usecase.SimulateGameUseCase;
import com.example.baseballorders.simulator.application.usecase.SimulationRunMode;
import com.example.baseballorders.simulator.domain.player.LineUpEntity;
import com.example.baseballorders.simulator.domain.statistics.GameStatisticsBuilder;
import com.example.baseballorders.simulator.domain.statistics.ScoreAccumulator;
import com.example.baseballorders.simulator.domain.statistics.ScoreStatistics;
import com.example.baseballorders.simulator.infrastructure.config.SimulationGrpcProperties;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 実物: GrpcServerLifecycle、GrpcSimulationService、Nettyのgrpcサーバー、実TCP接続のgRPCクライアント。 モック:
 * SimulateGameUseCase と LineUpMapper。 担保する疎通: gRPCクライアント -> TCP -> GrpcServerLifecycleが起動したサーバー ->
 * GrpcSimulationService。 担保しないもの: 試合計算、実際の打順変換、SpringコンテキストによるBean配線。
 */
class GrpcServerLifecycleTest {

    private final SimulateGameUseCase useCase = mock(SimulateGameUseCase.class);
    private final LineUpMapper mapper = mock(LineUpMapper.class);

    @Test
    @DisplayName("起動したサーバーが実RPCに応答し停止後は応答しない")
    void servesRealCallThenStops() throws Exception {
        // given
        var lineUp = mock(LineUpEntity.class);
        when(mapper.map(any())).thenReturn(lineUp);
        when(useCase.invoke(eq(lineUp), eq(SimulationRunMode.SINGLE_GAME_RUN)))
                .thenReturn(
                        SimulationResultBuilder.simulationResult()
                                .statistics(oneGameStatistics())
                                .transitions(List.of())
                                .build());
        var sut =
                new GrpcServerLifecycle(
                        new SimulationGrpcProperties(0, Duration.ofSeconds(2)),
                        new GrpcSimulationService(useCase, mapper));
        var request =
                SimulationRequestMessage.newBuilder()
                        .setSimulationId("lifecycle")
                        .setMode(SimulationMode.SIMULATION_MODE_SINGLE_GAME_RUN)
                        .build();

        // when
        boolean runningBeforeStart = sut.isRunning();
        sut.start();
        sut.start();
        boolean runningAfterStart = sut.isRunning();
        ManagedChannel channel =
                Grpc.newChannelBuilder(
                                "localhost:" + sut.getPort(), InsecureChannelCredentials.create())
                        .build();
        String echoedId;
        try {
            echoedId =
                    SimulationServiceGrpc.newBlockingStub(channel)
                            .simulate(request)
                            .getSimulationId();
        } finally {
            channel.shutdownNow();
        }
        sut.stop();
        sut.stop();

        // then
        assertAll(
                () -> assertFalse(runningBeforeStart),
                () -> assertTrue(runningAfterStart),
                () -> assertEquals("lifecycle", echoedId),
                () -> assertFalse(sut.isRunning()),
                () -> assertThrows(IllegalStateException.class, sut::getPort));
    }

    @Test
    @DisplayName("猶予時間を過ぎても完了しないRPCは強制終了してサーバーを止める")
    void forcesShutdownWhenGracePeriodExpires() throws Exception {
        // given
        var started = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        var lineUp = mock(LineUpEntity.class);
        when(mapper.map(any())).thenReturn(lineUp);
        when(useCase.invoke(any(LineUpEntity.class), any(SimulationRunMode.class)))
                .thenAnswer(
                        invocation -> {
                            started.countDown();
                            release.await(30, TimeUnit.SECONDS);
                            throw new IllegalStateException("released after shutdown");
                        });
        var sut =
                new GrpcServerLifecycle(
                        new SimulationGrpcProperties(0, Duration.ofMillis(100)),
                        new GrpcSimulationService(useCase, mapper));
        sut.start();
        ManagedChannel channel =
                Grpc.newChannelBuilder(
                                "localhost:" + sut.getPort(), InsecureChannelCredentials.create())
                        .build();
        var future =
                SimulationServiceGrpc.newFutureStub(channel)
                        .simulate(SimulationRequestMessage.getDefaultInstance());
        assertTrue(started.await(10, TimeUnit.SECONDS));

        // when
        sut.stop();
        release.countDown();
        var exception =
                assertThrows(
                        java.util.concurrent.ExecutionException.class,
                        () -> future.get(10, TimeUnit.SECONDS));
        channel.shutdownNow();

        // then
        assertAll(
                () -> assertFalse(sut.isRunning()),
                () ->
                        assertEquals(
                                Status.Code.CANCELLED,
                                ((StatusRuntimeException) exception.getCause())
                                        .getStatus()
                                        .getCode()));
    }

    private static ScoreStatistics oneGameStatistics() {
        var accumulator = new ScoreAccumulator();
        accumulator.onGameCompleted(
                0,
                GameStatisticsBuilder.gameStatistics()
                        .hitCount(0)
                        .singleHitCount(0)
                        .doubleHitCount(0)
                        .tripleHitCount(0)
                        .homeRunCount(0)
                        .soloHomeRunCount(0)
                        .twoRunHomeRunCount(0)
                        .threeRunHomeRunCount(0)
                        .grandSlamCount(0)
                        .buntCount(0)
                        .stealCount(0)
                        .buntFailureCount(0)
                        .stealFailureCount(0)
                        .advancingBuntCount(0)
                        .squeezeBuntCount(0)
                        .advancingBuntFailureCount(0)
                        .squeezeBuntFailureCount(0)
                        .stealToSecondCount(0)
                        .stealToThirdCount(0)
                        .build());
        return accumulator.toScoreStatistics();
    }
}

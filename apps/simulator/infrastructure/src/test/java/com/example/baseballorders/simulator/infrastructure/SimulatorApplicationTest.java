package com.example.baseballorders.simulator.infrastructure;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.baseballorders.messaging.PlayerPersonality;
import com.example.baseballorders.messaging.SimulationMode;
import com.example.baseballorders.messaging.SimulationPlayerMessage;
import com.example.baseballorders.messaging.SimulationRequestMessage;
import com.example.baseballorders.messaging.SimulationServiceGrpc;
import com.example.baseballorders.simulator.SimulatorApplication;
import com.example.baseballorders.simulator.infrastructure.messaging.GrpcServerLifecycle;
import com.example.baseballorders.simulator.infrastructure.messaging.LineUpMapper;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import java.util.Collections;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * 実物: SimulatorのSpring Boot設定、アプリケーション・ドメインBean、gRPCサーバー、実TCP接続のgRPCクライアント、試合計算。 モック: なし。 担保する疎通:
 * 起動エントリーポイント -> Bean構築 -> gRPCサーバー起動 -> gRPCクライアント -> LineUpMapper -> SimulateGameUseCase -> 結果。
 * 担保しないもの: backendからの呼び出し、本番プロファイルの値、Fargate上のネットワーク。
 */
class SimulatorApplicationTest {

    @Test
    @DisplayName("Simulatorを単独起動するとgRPCで1試合実行の要求に応答しコンテキスト終了で停止する")
    void startsAndServesGrpc() {
        // given
        var request =
                SimulationRequestMessage.newBuilder()
                        .setSimulationId("app-test")
                        .setMode(SimulationMode.SIMULATION_MODE_SINGLE_GAME_RUN)
                        .addAllPlayers(
                                Collections.nCopies(
                                        9,
                                        SimulationPlayerMessage.newBuilder()
                                                .setName("player")
                                                .setHitAverage(0.3f)
                                                .setPersonality(
                                                        PlayerPersonality
                                                                .PLAYER_PERSONALITY_MIDDLE_DISTANCE)
                                                .build()))
                        .build();
        GrpcServerLifecycle lifecycle;
        String echoedId;
        int transitionCount;
        int gameCount;
        boolean mapperPresent;

        // when
        try (var context =
                new SpringApplicationBuilder(SimulatorApplication.class)
                        .run("--simulation.grpc.port=0")) {
            lifecycle = context.getBean(GrpcServerLifecycle.class);
            mapperPresent = context.getBean(LineUpMapper.class) != null;
            ManagedChannel channel =
                    Grpc.newChannelBuilder(
                                    "localhost:" + lifecycle.getPort(),
                                    InsecureChannelCredentials.create())
                            .build();
            try {
                var result = SimulationServiceGrpc.newBlockingStub(channel).simulate(request);
                echoedId = result.getSimulationId();
                transitionCount = result.getGameTransitionsCount();
                gameCount = result.getGameScoreStatistics().getGameCount();
            } finally {
                channel.shutdownNow();
            }
            assertTrue(lifecycle.isRunning());
        }

        // then
        var stoppedLifecycle = lifecycle;
        assertAll(
                () -> assertTrue(mapperPresent),
                () -> assertNotNull(stoppedLifecycle),
                () -> assertEquals("app-test", echoedId),
                () -> assertEquals(1, gameCount),
                () -> assertTrue(transitionCount > 0),
                () -> assertFalse(stoppedLifecycle.isRunning()));
    }
}

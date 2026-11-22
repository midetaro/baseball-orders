package com.example.baseballorders.messaging;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SimulationContractTest {

    @Test
    @DisplayName("未指定の列挙値は従来の既定値（大規模実行・単打マン・既定投手）として読み取られる")
    void unsetEnumsReadAsLegacyDefaults() throws InvalidProtocolBufferException {
        // given
        byte[] wire =
                SimulationRequestMessage.newBuilder()
                        .setSimulationId("id")
                        .addPlayers(SimulationPlayerMessage.newBuilder().setName("a"))
                        .build()
                        .toByteArray();

        // when
        var request = SimulationRequestMessage.parseFrom(wire);

        // then
        assertAll(
                () ->
                        assertEquals(
                                SimulationMode.SIMULATION_MODE_LARGE_SCALE_RUN, request.getMode()),
                () ->
                        assertEquals(
                                PitcherPersonality.PITCHER_PERSONALITY_DEFAULT,
                                request.getPitcherPersonality()),
                () ->
                        assertEquals(
                                PlayerPersonality.PLAYER_PERSONALITY_DEFAULT,
                                request.getPlayers(0).getPersonality()));
    }

    @Test
    @DisplayName("結果メッセージは統計と状況推移をワイヤー上で往復できる")
    void resultRoundTripsStatisticsAndTransitions() throws InvalidProtocolBufferException {
        // given
        var result =
                SimulationResultMessage.newBuilder()
                        .setSimulationId("id")
                        .setGameScoreStatistics(
                                GameScoreStatistics.newBuilder()
                                        .setAverageScore(3.5)
                                        .setMedianScore(3.0)
                                        .setMaximumScore(12)
                                        .setGameCount(100)
                                        .putScoreDistribution(3, 20))
                        .setGameContentStatistics(
                                GameContentStatistics.newBuilder()
                                        .setHitCount(9)
                                        .setStealToThirdCount(1))
                        .addGameTransitions(
                                GameTransitionMessage.newBuilder()
                                        .setInning(1)
                                        .setActionResult("HIT")
                                        .setOutCount(0)
                                        .setCumulativeScore(1)
                                        .setRunnerState("FIRST"))
                        .build();

        // when
        var parsed = SimulationResultMessage.parseFrom(result.toByteArray());

        // then
        assertAll(
                () -> assertEquals(result, parsed),
                () ->
                        assertEquals(
                                20, parsed.getGameScoreStatistics().getScoreDistributionOrThrow(3)),
                () -> assertEquals("FIRST", parsed.getGameTransitions(0).getRunnerState()));
    }

    @Test
    @DisplayName("SimulationServiceは要求を受けて結果を返すunary RPCのSimulateを公開する")
    void serviceExposesUnarySimulate() {
        // when
        var method = SimulationServiceGrpc.getSimulateMethod();

        // then
        assertAll(
                () -> assertNotNull(method),
                () -> assertEquals(io.grpc.MethodDescriptor.MethodType.UNARY, method.getType()),
                () ->
                        assertEquals(
                                "baseballorders.messaging.SimulationService/Simulate",
                                method.getFullMethodName()));
    }
}

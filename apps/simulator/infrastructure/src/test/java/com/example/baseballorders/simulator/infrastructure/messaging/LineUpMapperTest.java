package com.example.baseballorders.simulator.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import com.example.baseballorders.messaging.PlayerPersonality;
import com.example.baseballorders.messaging.SimulationPlayerMessage;
import com.example.baseballorders.simulator.domain.play.*;
import com.example.baseballorders.simulator.domain.player.strategy.RandomGenerator;
import com.example.baseballorders.simulator.domain.player.strategy.batting.HittingStrategy;
import com.example.baseballorders.simulator.domain.player.strategy.batting.MiddleDistanceHittingStrategy;
import com.example.baseballorders.simulator.domain.player.strategy.bunt.StandardBuntStrategy;
import com.example.baseballorders.simulator.domain.player.strategy.steal.StandardStealStrategy;
import com.example.baseballorders.simulator.domain.rule.SimulationRulesTestData;
import com.example.baseballorders.simulator.domain.statistics.GameStatisticsRecorder;
import com.example.baseballorders.simulator.infrastructure.config.SimulationPitcherProperties;
import com.example.baseballorders.simulator.infrastructure.config.SimulationPitcherProperties.Multipliers;
import com.example.baseballorders.simulator.infrastructure.config.SimulationPropertiesTestData;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

class LineUpMapperTest {

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(
            value = PlayerPersonality.class,
            mode = org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE,
            names = "UNRECOGNIZED")
    @DisplayName("選手の性格に対応する既存の行動戦略を適用する")
    void mapsPersonalityToBehavior(PlayerPersonality personality) {
        // given
        var sut =
                new LineUpMapper(
                        SimulationRulesTestData.strategies().middleDistanceHittingStrategy(),
                        SimulationRulesTestData.strategies().eagerSteal(),
                        SimulationRulesTestData.strategies().standardBunt(),
                        SimulationRulesTestData.strategies(),
                        SimulationPropertiesTestData.standardPitcherProperties());
        var player = player(0.3f, true, true, personality);

        // when
        List<BattingResult> battingResults;
        StealResult stealResult;
        BuntResult buntResult;
        try (MockedStatic<RandomGenerator> randomGenerator = mockStatic(RandomGenerator.class)) {
            // 打率0.3で0.24と0.32の2打席の結果は、短距離戦略では単打・二塁打、中距離戦略では二塁打・本塁打、
            // 長距離戦略では三塁打・本塁打、高出塁率戦略では単打・単打となり、打撃戦略の選択を識別する。
            randomGenerator.when(RandomGenerator::nextFloat).thenReturn(0.24f, 0.32f, 0.9f, 0.9f);
            var batter =
                    sut.map(java.util.Collections.nCopies(9, player))
                            .getBatterEntities()
                            .getFirst();
            battingResults = List.of(batter.swing(0), batter.swing(0));
            stealResult = batter.stealToDouble();
            buntResult = batter.bunt(OutCount.ONE_OUT, BuntType.ADVANCING);
        }

        // then
        assertAll(
                () ->
                        assertEquals(
                                switch (personality) {
                                    case PLAYER_PERSONALITY_DEFAULT ->
                                            List.of(
                                                    BattingResult.HIT_SINGLE,
                                                    BattingResult.HIT_DOUBLE);
                                    case PLAYER_PERSONALITY_MIDDLE_DISTANCE,
                                            PLAYER_PERSONALITY_EAGER_STEAL,
                                            PLAYER_PERSONALITY_EAGER_BUNT ->
                                            List.of(
                                                    BattingResult.HIT_DOUBLE,
                                                    BattingResult.HIT_HOMER);
                                    case PLAYER_PERSONALITY_EAGER_SLUGGISH ->
                                            List.of(
                                                    BattingResult.HIT_TRIPLE,
                                                    BattingResult.HIT_HOMER);
                                    case PLAYER_PERSONALITY_HIGH_ON_BASE ->
                                            List.of(
                                                    BattingResult.HIT_SINGLE,
                                                    BattingResult.HIT_SINGLE);
                                    case UNRECOGNIZED ->
                                            throw new IllegalStateException(
                                                    "UNRECOGNIZED is excluded from the source");
                                },
                                battingResults),
                () -> assertEquals(StealResult.SUCCESS, stealResult),
                () ->
                        assertEquals(
                                personality == PlayerPersonality.PLAYER_PERSONALITY_EAGER_BUNT
                                        ? BuntResult.FAILURE
                                        : BuntResult.NOT_TRY,
                                buntResult));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
        "true,true",
        "true,false",
        "false,true",
        "false,false"
    })
    @DisplayName("盗塁とバントの実行選択を独立して反映する")
    void respectsOptionalStrategies(boolean stealEnabled, boolean buntEnabled) throws Exception {
        // given
        var player =
                player(
                        0.3f,
                        buntEnabled,
                        stealEnabled,
                        PlayerPersonality.PLAYER_PERSONALITY_DEFAULT);
        var sut =
                new LineUpMapper(
                        SimulationRulesTestData.strategies().middleDistanceHittingStrategy(),
                        SimulationRulesTestData.strategies().eagerSteal(),
                        SimulationRulesTestData.strategies().standardBunt(),
                        SimulationRulesTestData.strategies(),
                        SimulationPropertiesTestData.standardPitcherProperties());
        var statisticsRecorder = new GameStatisticsRecorder();

        // when
        StealResult doubleResult;
        StealResult tripleResult;
        BuntResult buntResult;
        try (MockedStatic<RandomGenerator> randomGenerator = mockStatic(RandomGenerator.class)) {
            if (stealEnabled) {
                randomGenerator.when(RandomGenerator::nextFloat).thenReturn(0.8f, 0.9f, 0.1f);
            } else {
                randomGenerator.when(RandomGenerator::nextFloat).thenReturn(0.1f);
            }
            var batter =
                    sut.map(java.util.Collections.nCopies(9, player))
                            .getBatterEntities()
                            .getFirst();
            var observedBatter = batter.observedBy(statisticsRecorder);
            doubleResult = observedBatter.stealToDouble();
            tripleResult = observedBatter.stealToTriple();
            buntResult = observedBatter.bunt(OutCount.NO_OUT, BuntType.ADVANCING);
        }

        // then
        assertAll(
                () ->
                        assertEquals(
                                stealEnabled ? StealResult.SUCCESS : StealResult.NOT_TRY,
                                doubleResult),
                () ->
                        assertEquals(
                                stealEnabled ? StealResult.SUCCESS : StealResult.NOT_TRY,
                                tripleResult),
                () ->
                        assertEquals(
                                buntEnabled ? BuntResult.SUCCESS : BuntResult.NOT_TRY, buntResult),
                () ->
                        assertEquals(
                                stealEnabled ? 2 : 0, statisticsRecorder.snapshot().stealCount()));
    }

    /**
     * 実物: SimulationPlayerMessage、LineUpMapper、BatterEntity、中距離打撃戦略。 モック: RandomGenerator
     * の乱数だけを固定する。 担保する疎通: gRPC選手メッセージ -> LineUpMapper -> BatterEntity -> 打撃戦略。 担保しないもの:
     * gRPCの送受信、試合全体の進行、乱数生成器自体の分布。
     */
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
        "0.01,WALK",
        "0.2,HIT_SINGLE",
        "0.34,STRIKEOUT",
        "0.8,BATTED_OUT"
    })
    @DisplayName("既存のhit_averageフィールドを四球を含めない打率として打撃戦略へ渡す")
    void mapsLegacyWireFieldAsBattingAverage(float draw, BattingResult expected) throws Exception {
        // given
        var player =
                player(0.3f, false, false, PlayerPersonality.PLAYER_PERSONALITY_MIDDLE_DISTANCE);
        var sut =
                new LineUpMapper(
                        SimulationRulesTestData.strategies().middleDistanceHittingStrategy(),
                        SimulationRulesTestData.strategies().eagerSteal(),
                        SimulationRulesTestData.strategies().standardBunt(),
                        SimulationRulesTestData.strategies(),
                        SimulationPropertiesTestData.standardPitcherProperties());

        // when
        BattingResult result;
        try (MockedStatic<RandomGenerator> randomGenerator = mockStatic(RandomGenerator.class)) {
            randomGenerator.when(RandomGenerator::nextFloat).thenReturn(draw);
            result =
                    sut.map(java.util.Collections.nCopies(9, player))
                            .getBatterEntities()
                            .getFirst()
                            .swing(0);
        }

        // then
        assertAll(() -> assertEquals(expected, result));
    }

    @Test
    @DisplayName("gRPCの選手情報を打順へ変換すると全選手の能力と振る舞いが保持される")
    void mapsGrpcPlayersToLineUpEntity() {
        // given
        HittingStrategy hittingStrategy =
                SimulationRulesTestData.strategies().middleDistanceHittingStrategy();
        LineUpMapper sut =
                new LineUpMapper(
                        hittingStrategy,
                        SimulationRulesTestData.strategies().eagerSteal(),
                        SimulationRulesTestData.strategies().standardBunt(),
                        SimulationRulesTestData.strategies(),
                        SimulationPropertiesTestData.standardPitcherProperties());
        List<SimulationPlayerMessage> players =
                IntStream.rangeClosed(1, 9)
                        .mapToObj(
                                number ->
                                        player(
                                                1.0f,
                                                false,
                                                true,
                                                PlayerPersonality.PLAYER_PERSONALITY_DEFAULT))
                        .toList();

        // when
        var result = sut.map(players);
        var statisticsRecorder = new GameStatisticsRecorder();
        BattingResult battingResult;
        StealResult stealResult;
        try (MockedStatic<RandomGenerator> randomGenerator = mockStatic(RandomGenerator.class)) {
            randomGenerator.when(RandomGenerator::nextFloat).thenReturn(0.1f, 0.8f);
            var observedBatter =
                    result.getBatterEntities().getFirst().observedBy(statisticsRecorder);
            battingResult = observedBatter.swing(0);
            stealResult = observedBatter.stealToDouble();
        }

        // then
        assertAll(
                () -> assertEquals(9, result.getBatterEntities().size()),
                () -> assertEquals(BattingResult.HIT_SINGLE, battingResult),
                () -> assertEquals(StealResult.SUCCESS, stealResult));
    }

    @Test
    @DisplayName("設定された既定の投手補正倍率で打率・長打率を補正する")
    void usesConfiguredPitcherMultipliers() {
        // given
        var hitting = mock(MiddleDistanceHittingStrategy.class);
        var stealing = mock(StandardStealStrategy.class);
        var bunting = mock(StandardBuntStrategy.class);
        var pitcherProperties = new SimulationPitcherProperties(new Multipliers(2.0f));
        var sut =
                new LineUpMapper(
                        hitting,
                        stealing,
                        bunting,
                        SimulationRulesTestData.strategies(),
                        pitcherProperties);
        var player = player(0.3f, true, true, PlayerPersonality.PLAYER_PERSONALITY_MIDDLE_DISTANCE);
        var battingAverageCaptor = ArgumentCaptor.forClass(Float.class);

        // when
        var batter =
                sut.map(java.util.Collections.nCopies(9, player)).getBatterEntities().getFirst();
        batter.swing(0);
        batter.bunt(OutCount.NO_OUT, BuntType.ADVANCING);
        batter.stealToDouble();
        verify(hitting).batting(battingAverageCaptor.capture());
        verify(bunting).bunt(OutCount.NO_OUT, BuntType.ADVANCING);
        verify(stealing).runToDouble();

        // then
        assertEquals(0.6f, battingAverageCaptor.getValue(), 0.00001f);
    }

    @Test
    @DisplayName("認識できない選手の性格を含む要求は不正な引数として拒否する")
    void rejectsUnrecognizedPersonality() {
        // given
        var sut =
                new LineUpMapper(
                        SimulationRulesTestData.strategies().middleDistanceHittingStrategy(),
                        SimulationRulesTestData.strategies().eagerSteal(),
                        SimulationRulesTestData.strategies().standardBunt(),
                        SimulationRulesTestData.strategies(),
                        SimulationPropertiesTestData.standardPitcherProperties());
        var unrecognized = SimulationPlayerMessage.newBuilder().setPersonalityValue(99).build();
        var players = java.util.Collections.nCopies(9, unrecognized);

        // when
        var exception = assertThrows(IllegalArgumentException.class, () -> sut.map(players));

        // then
        assertAll(() -> assertTrue(exception.getMessage().contains("UNRECOGNIZED")));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"true,false", "false,true"})
    @DisplayName("盗塁またはバントが有効なとき認識できない性格を拒否する")
    void rejectsUnrecognizedPersonalityPerStrategy(boolean stealEnabled, boolean buntEnabled) {
        // given
        var sut =
                new LineUpMapper(
                        SimulationRulesTestData.strategies().middleDistanceHittingStrategy(),
                        SimulationRulesTestData.strategies().eagerSteal(),
                        SimulationRulesTestData.strategies().standardBunt(),
                        SimulationRulesTestData.strategies(),
                        SimulationPropertiesTestData.standardPitcherProperties());
        var unrecognized =
                SimulationPlayerMessage.newBuilder()
                        .setStealEnabled(stealEnabled)
                        .setBuntEnabled(buntEnabled)
                        .setPersonalityValue(99)
                        .build();
        var players = java.util.Collections.nCopies(9, unrecognized);

        // when
        var exception = assertThrows(IllegalArgumentException.class, () -> sut.map(players));

        // then
        assertAll(() -> assertTrue(exception.getMessage().contains("UNRECOGNIZED")));
    }

    private static SimulationPlayerMessage player(
            float hitAverage,
            boolean buntEnabled,
            boolean stealEnabled,
            PlayerPersonality personality) {
        return SimulationPlayerMessage.newBuilder()
                .setName("1番")
                .setHitAverage(hitAverage)
                .setBuntEnabled(buntEnabled)
                .setStealEnabled(stealEnabled)
                .setPersonality(personality)
                .build();
    }
}

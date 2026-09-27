package com.example.baseballorders.simulator.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    @org.junit.jupiter.params.provider.EnumSource(PlayerPersonality.class)
    @DisplayName("選手の性格に対応する既存の行動戦略を適用する")
    void mapsPersonalityToBehavior(PlayerPersonality personality) {
        // given
        var mapper =
                new LineUpMapper(
                        SimulationRulesTestData.strategies().middleDistanceHittingStrategy(),
                        SimulationRulesTestData.strategies().eagerSteal(),
                        SimulationRulesTestData.strategies().standardBunt(),
                        SimulationRulesTestData.strategies(),
                        SimulationPropertiesTestData.standardPitcherProperties());
        var player =
                new SimulationPlayerMessage("1番", 0.3f, 0.4f, 0.0f, true, 0.8f, true, personality);

        // when
        BattingResult battingResult;
        StealResult stealResult;
        BuntResult buntResult;
        try (MockedStatic<RandomGenerator> randomGenerator = mockStatic(RandomGenerator.class)) {
            randomGenerator
                    .when(RandomGenerator::nextFloat)
                    .thenReturn(
                            personality == PlayerPersonality.EAGER_SLUGGISH ? 0.27f : 0.8f,
                            0.9f,
                            0.1f);
            var batter =
                    mapper.map(java.util.Collections.nCopies(9, player))
                            .getBatterEntities()
                            .getFirst();
            battingResult = batter.swing(0);
            stealResult = batter.stealToDouble();
            buntResult = batter.bunt(OutCount.ONE_OUT, BuntType.ADVANCING);
        }

        // then
        assertAll(
                () ->
                        assertEquals(
                                personality == PlayerPersonality.EAGER_SLUGGISH
                                        ? BattingResult.HIT_HOMER
                                        : BattingResult.BATTED_OUT,
                                battingResult),
                () -> assertEquals(StealResult.SUCCESS, stealResult),
                () ->
                        assertEquals(
                                personality == PlayerPersonality.EAGER_BUNT
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
                new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(
                                """
                                        {"name":"1番","hitAverage":0.3,"sluggish":0.4,"buntSuccessRate":0.7,
                                         "buntEnabled":%s,"stealSuccessRate":0.8,"stealEnabled":%s}
                                        """
                                        .formatted(buntEnabled, stealEnabled),
                                SimulationPlayerMessage.class);
        var mapper =
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
                    mapper.map(java.util.Collections.nCopies(9, player))
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

    @Test
    @DisplayName("SQSの選手情報を打順へ変換すると全選手の能力と振る舞いが保持される")
    void mapsSqsPlayersToLineUpEntity() {
        // given
        HittingStrategy hittingStrategy =
                SimulationRulesTestData.strategies().middleDistanceHittingStrategy();
        LineUpMapper mapper =
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
                                        new SimulationPlayerMessage(
                                                "player-" + number,
                                                1.0f,
                                                0.0f,
                                                0.8f,
                                                false,
                                                0.9f,
                                                true))
                        .toList();

        // when
        var result = mapper.map(players);
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
    @DisplayName("設定された既定の投手補正倍率で各確率を補正する")
    void usesConfiguredPitcherMultipliers() {
        // given
        var hitting = mock(MiddleDistanceHittingStrategy.class);
        var stealing = mock(StandardStealStrategy.class);
        var bunting = mock(StandardBuntStrategy.class);
        var pitcherProperties = new SimulationPitcherProperties(new Multipliers(2.0f, 0.5f, 0.25f));
        var mapper =
                new LineUpMapper(
                        hitting,
                        stealing,
                        bunting,
                        SimulationRulesTestData.strategies(),
                        pitcherProperties);
        var player =
                new SimulationPlayerMessage(
                        "1番", 0.3f, 0.4f, 0.5f, true, 0.6f, true, PlayerPersonality.DEFAULT);
        var onBaseCaptor = ArgumentCaptor.forClass(Float.class);
        var sluggingCaptor = ArgumentCaptor.forClass(Float.class);
        var buntCaptor = ArgumentCaptor.forClass(Float.class);
        var stealCaptor = ArgumentCaptor.forClass(Float.class);

        // when
        var batter =
                mapper.map(java.util.Collections.nCopies(9, player)).getBatterEntities().getFirst();
        batter.swing(0);
        batter.bunt(OutCount.NO_OUT, BuntType.ADVANCING);
        batter.stealToDouble();
        verify(hitting).batting(onBaseCaptor.capture(), sluggingCaptor.capture());
        verify(bunting)
                .bunt(buntCaptor.capture(), org.mockito.ArgumentMatchers.eq(OutCount.NO_OUT));
        verify(stealing).runToDouble(stealCaptor.capture());

        // then
        assertAll(
                () -> assertEquals(0.6f, onBaseCaptor.getValue(), 0.00001f),
                () -> assertEquals(0.2f, sluggingCaptor.getValue(), 0.00001f),
                () -> assertEquals(0.125f, buntCaptor.getValue(), 0.00001f),
                () -> assertEquals(0.15f, stealCaptor.getValue(), 0.00001f));
    }
}

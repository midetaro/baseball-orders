package com.example.baseballorders.simulator.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

import com.example.baseballorders.messaging.PlayerPersonality;
import com.example.baseballorders.messaging.SimulationPlayerMessage;
import com.example.baseballorders.simulator.domain.play.BattingResult;
import com.example.baseballorders.simulator.domain.play.BuntResult;
import com.example.baseballorders.simulator.domain.play.OutCount;
import com.example.baseballorders.simulator.domain.play.StealResult;
import com.example.baseballorders.simulator.domain.player.strategy.BehaviorStrategies;
import com.example.baseballorders.simulator.domain.player.strategy.RandomGenerator;
import com.example.baseballorders.simulator.domain.player.strategy.batting.HittingStrategy;
import com.example.baseballorders.simulator.domain.statistics.GameStatisticsRecorder;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class LineUpMapperTest {

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(PlayerPersonality.class)
    @DisplayName("選手の性格に対応する既存の行動戦略を適用する")
    void mapsPersonalityToBehavior(PlayerPersonality personality) {
        // given
        var mapper =
                new LineUpMapper(
                        BehaviorStrategies.middleDistanceHittingStrategy(),
                        BehaviorStrategies.eagerSteal(),
                        BehaviorStrategies.standardBunt());
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
                            personality == PlayerPersonality.EAGER_SLUGGISH ? 0.23f : 0.8f,
                            0.9f,
                            0.1f);
            var batter =
                    mapper.map(java.util.Collections.nCopies(9, player))
                            .getBatterEntities()
                            .getFirst();
            battingResult = batter.swing(0);
            stealResult = batter.stealToDouble();
            buntResult = batter.bunt(OutCount.ONE_OUT);
        }

        // then
        assertAll(
                () ->
                        assertEquals(
                                personality == PlayerPersonality.EAGER_SLUGGISH
                                        ? BattingResult.HIT_HOMER
                                        : BattingResult.OUT,
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
                        BehaviorStrategies.middleDistanceHittingStrategy(),
                        BehaviorStrategies.eagerSteal(),
                        BehaviorStrategies.standardBunt());
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
            buntResult = observedBatter.bunt(OutCount.NO_OUT);
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
        HittingStrategy hittingStrategy = BehaviorStrategies.middleDistanceHittingStrategy();
        LineUpMapper mapper =
                new LineUpMapper(
                        hittingStrategy,
                        BehaviorStrategies.eagerSteal(),
                        BehaviorStrategies.standardBunt());
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
}

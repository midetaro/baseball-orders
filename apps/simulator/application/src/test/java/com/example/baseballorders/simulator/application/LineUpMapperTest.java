package com.example.baseballorders.simulator.application;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

import com.example.baseballorders.messaging.PlayerPersonality;
import com.example.baseballorders.messaging.SimulationPlayerMessage;
import com.example.baseballorders.simulator.domain.code.BattingResult;
import com.example.baseballorders.simulator.domain.code.BuntResult;
import com.example.baseballorders.simulator.domain.code.OutCount;
import com.example.baseballorders.simulator.domain.code.StealResult;
import com.example.baseballorders.simulator.domain.model.behavior.AtBatBehavior;
import com.example.baseballorders.simulator.domain.model.behavior.EagerStealBehavior;
import com.example.baseballorders.simulator.domain.model.behavior.MiddleDistanceBattingBehavior;
import com.example.baseballorders.simulator.domain.model.behavior.StandardBuntStrategy;
import com.example.baseballorders.simulator.domain.model.state.SingleBasesState;
import com.example.baseballorders.simulator.domain.model.statistics.GameStatisticsRecorder;
import com.example.baseballorders.simulator.domain.util.RandomGenerator;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Qualifier;

class LineUpMapperTest {

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(PlayerPersonality.class)
    @DisplayName("選手の性格に対応する既存の行動戦略を適用する")
    void mapsPersonalityToBehavior(PlayerPersonality personality) {
        // given
        var mapper =
                new LineUpMapper(
                        new MiddleDistanceBattingBehavior(),
                        new EagerStealBehavior(),
                        new StandardBuntStrategy());
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
            buntResult = batter.bunt(OutCount.ONE_OUT, new SingleBasesState(batter));
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
                        new MiddleDistanceBattingBehavior(),
                        new EagerStealBehavior(),
                        new StandardBuntStrategy());
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
            buntResult = observedBatter.bunt(OutCount.NO_OUT, new SingleBasesState(observedBatter));
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
    @DisplayName("既定の打撃戦略には中距離バッターを指定する")
    void usesMiddleDistanceAtBatAsDefault() {
        // given
        var constructor = LineUpMapper.class.getConstructors()[0];

        // when
        Qualifier qualifier = constructor.getParameters()[0].getAnnotation(Qualifier.class);

        // then
        assertAll(
                () -> assertEquals("middleDistanceAtBat", qualifier.value()),
                () -> assertEquals(AtBatBehavior.class, constructor.getParameters()[0].getType()));
    }

    @Test
    @DisplayName("SQSの選手情報を打順へ変換すると全選手の能力と振る舞いが保持される")
    void mapsSqsPlayersToLineUpEntity() {
        // given
        AtBatBehavior atBatBehavior = new MiddleDistanceBattingBehavior();
        LineUpMapper mapper =
                new LineUpMapper(
                        atBatBehavior, new EagerStealBehavior(), new StandardBuntStrategy());
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

package com.example.baseballorders.simulator.application;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.baseballorders.messaging.SimulationPlayerMessage;
import com.example.baseballorders.simulator.domain.code.BattingResult;
import com.example.baseballorders.simulator.domain.code.BuntResult;
import com.example.baseballorders.simulator.domain.code.OutCount;
import com.example.baseballorders.simulator.domain.code.StealResult;
import com.example.baseballorders.simulator.domain.model.behavior.AtBatBehavior;
import com.example.baseballorders.simulator.domain.model.behavior.BuntStrategy;
import com.example.baseballorders.simulator.domain.model.behavior.StealStrategy;
import com.example.baseballorders.simulator.domain.model.state.SingleBasesState;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;

class LineUpMapperTest {

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
        var strategy = org.mockito.Mockito.mock(StealStrategy.class);
        org.mockito.Mockito.when(strategy.runToDouble(0.8f)).thenReturn(StealResult.SUCCESS);
        org.mockito.Mockito.when(strategy.runToTriple(0.8f)).thenReturn(StealResult.SUCCESS);
        var buntStrategy = org.mockito.Mockito.mock(BuntStrategy.class);
        org.mockito.Mockito.when(
                        buntStrategy.bunt(
                                org.mockito.ArgumentMatchers.eq(0.7f),
                                org.mockito.ArgumentMatchers.eq(OutCount.NO_OUT),
                                org.mockito.ArgumentMatchers.any(SingleBasesState.class)))
                .thenReturn(BuntResult.SUCCESS);
        var mapper = new LineUpMapper((hit, slug) -> BattingResult.OUT, strategy, buntStrategy);

        // when
        var batter =
                mapper.map(java.util.Collections.nCopies(9, player)).getBatterEntities().getFirst();
        var doubleResult = batter.stealToDouble();
        var tripleResult = batter.stealToTriple();
        var buntResult = batter.bunt(OutCount.NO_OUT, new SingleBasesState());
        var mappedBuntStrategyResult =
                batter.getBuntStrategy().bunt(0.7f, OutCount.NO_OUT, new SingleBasesState());

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
                                buntEnabled ? BuntResult.SUCCESS : BuntResult.NOT_TRY,
                                mappedBuntStrategyResult),
                () -> {
                    if (!stealEnabled) org.mockito.Mockito.verifyNoInteractions(strategy);
                });
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
        AtBatBehavior atBatBehavior = (onBasePercentage, sluggish) -> BattingResult.HIT_SINGLE;
        FixedStealStrategy stealStrategy = new FixedStealStrategy();
        LineUpMapper mapper =
                new LineUpMapper(
                        atBatBehavior,
                        stealStrategy,
                        (successRate, outCounts, basesState) -> BuntResult.SUCCESS);
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

        // then
        assertAll(
                () -> assertEquals(9, result.getBatterEntities().size()),
                () ->
                        assertEquals(
                                1.0f, result.getBatterEntities().getFirst().getOnBasePercentage()),
                () ->
                        assertEquals(
                                BattingResult.HIT_SINGLE,
                                result.getBatterEntities().getFirst().swing()),
                () ->
                        assertEquals(
                                StealResult.NOT_TRY,
                                result.getBatterEntities().getFirst().stealToDouble()),
                () -> assertEquals(0.9f, stealStrategy.receivedSuccessRate),
                () ->
                        assertEquals(
                                BuntResult.NOT_TRY,
                                result.getBatterEntities()
                                        .getFirst()
                                        .bunt(OutCount.NO_OUT, new SingleBasesState())));
    }

    private static final class FixedStealStrategy implements StealStrategy {

        private float receivedSuccessRate;

        @Override
        public StealResult runToDouble(float successRate) {
            receivedSuccessRate = successRate;
            return StealResult.NOT_TRY;
        }

        @Override
        public StealResult runToTriple(float successRate) {
            return StealResult.NOT_TRY;
        }
    }
}

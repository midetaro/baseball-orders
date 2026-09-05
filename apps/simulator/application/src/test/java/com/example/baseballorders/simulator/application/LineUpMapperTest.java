package com.example.baseballorders.simulator.application;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.baseballorders.messaging.SimulationPlayerMessage;
import com.example.baseballorders.simulator.domain.code.BattingResult;
import com.example.baseballorders.simulator.domain.code.BuntResult;
import com.example.baseballorders.simulator.domain.code.OutCount;
import com.example.baseballorders.simulator.domain.code.StealResult;
import com.example.baseballorders.simulator.domain.model.behavior.AtBatBehavior;
import com.example.baseballorders.simulator.domain.model.behavior.StealStrategy;
import com.example.baseballorders.simulator.domain.model.state.SingleBasesState;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LineUpMapperTest {

    @Test
    @DisplayName("SQSの選手情報を打順へ変換すると全選手の能力と振る舞いが保持される")
    void mapsSqsPlayersToLineUpEntity() {
        // given
        AtBatBehavior atBatBehavior = (hitAverage, sluggish) -> BattingResult.HIT_SINGLE;
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
                                                "player-" + number, 1.0f, 0.0f, 0.8f, 0.9f))
                        .toList();

        // when
        var result = mapper.map(players);

        // then
        assertAll(
                () -> assertEquals(9, result.getBatterEntities().size()),
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
                                BuntResult.SUCCESS,
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

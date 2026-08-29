package com.example.baseballorders.simulator.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.baseballorders.simulator.domain.code.BattingResult;
import com.example.baseballorders.simulator.domain.code.StealResult;
import com.example.baseballorders.simulator.domain.model.behavior.AtBatBehavior;
import com.example.baseballorders.simulator.domain.model.behavior.StealStrategy;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class LineUpMapperTest {

    @Test
    void mapsSqsPlayersToLineUpEntity() {
        AtBatBehavior atBatBehavior = (hitAverage, sluggish) -> BattingResult.HIT_SINGLE;
        StealStrategy stealStrategy = new FixedStealStrategy();
        LineUpMapper mapper = new LineUpMapper(atBatBehavior, stealStrategy);
        List<PlayerData> players =
                IntStream.rangeClosed(1, 9)
                        .mapToObj(number -> new PlayerData("player-" + number, 1.0f, 0.0f))
                        .toList();

        var lineUp = mapper.map(players);

        assertEquals(9, lineUp.getBatterEntities().size());
        assertEquals(BattingResult.HIT_SINGLE, lineUp.getBatterEntities().getFirst().swing());
        assertEquals(StealResult.NOT_TRY, lineUp.getBatterEntities().getFirst().stealToDouble());
    }

    private static final class FixedStealStrategy implements StealStrategy {

        @Override
        public StealResult runToDouble() {
            return StealResult.NOT_TRY;
        }

        @Override
        public StealResult runToTriple() {
            return StealResult.NOT_TRY;
        }
    }
}

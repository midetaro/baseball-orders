package com.example.baseballorders.simulator.domain.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.baseballorders.simulator.domain.code.Base;
import com.example.baseballorders.simulator.domain.code.BattingResult;
import com.example.baseballorders.simulator.domain.code.BuntResult;
import com.example.baseballorders.simulator.domain.code.StealResult;
import com.example.baseballorders.simulator.domain.model.behavior.StealStrategy;
import com.example.baseballorders.simulator.domain.model.player.BatterEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BaseRunnersTest {

    @Test
    @DisplayName("走者を進めると塁の位置をまとめて更新する")
    void advancesRunners() {
        // given
        BatterEntity firstRunner = batter("first");
        BatterEntity secondRunner = batter("second");
        BaseRunners runners = new BaseRunners(firstRunner, secondRunner, null);

        // when
        BaseRunners advanced = runners.advance(Base.FIRST);

        // then
        assertAll(
                () -> assertEquals(null, advanced.getFirst()),
                () -> assertEquals(firstRunner, advanced.getSecond()),
                () -> assertEquals(secondRunner, advanced.getThird()),
                () -> assertEquals(2, advanced.count()));
    }

    private static BatterEntity batter(String name) {
        return new BatterEntity(
                name,
                0.3f,
                0.4f,
                0.7f,
                0.8f,
                (onBasePercentage, slugging) -> BattingResult.OUT,
                new NeverStealStrategy(),
                (successRate, outCount, basesState) -> BuntResult.NOT_TRY);
    }

    private static final class NeverStealStrategy implements StealStrategy {

        @Override
        public StealResult runToDouble(float successRate) {
            return StealResult.NOT_TRY;
        }

        @Override
        public StealResult runToTriple(float successRate) {
            return StealResult.NOT_TRY;
        }
    }
}

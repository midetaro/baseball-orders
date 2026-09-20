package com.example.baseballorders.simulator.domain.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.baseballorders.simulator.domain.code.Base;
import com.example.baseballorders.simulator.domain.code.OutCount;
import com.example.baseballorders.simulator.domain.entity.behavior.BehaviorStrategies;
import com.example.baseballorders.simulator.domain.entity.behavior.bunt.BuntStrategy;
import com.example.baseballorders.simulator.domain.entity.player.BatterEntity;
import com.example.baseballorders.simulator.domain.entity.player.LineUpEntity;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AtBatProcessorTest {

    @Test
    @DisplayName("バント機会がなければバントせず打撃する")
    void swingsWithoutBuntOpportunity() {
        // given
        var batter = batter(1.0f, BehaviorStrategies.standardBunt());
        var context = new GameBattingContext(new LineUpEntity(List.of(batter)));

        // when
        var completed = new AtBatProcessor().process(context, batter);

        // then
        assertAll(
                () -> assertTrue(completed),
                () -> assertEquals(OutCount.ONE_OUT, context.getCurrentState().getOutCount()),
                () -> assertFalse(context.isBuntable()));
    }

    @Test
    @DisplayName("無死でバントが成功すると走者を進めて打撃しない")
    void appliesSuccessfulBuntWithoutSwinging() {
        // given
        var runner = batter(0.0f, BehaviorStrategies.noBunt());
        var batter = batter(1.0f, BehaviorStrategies.standardBunt());
        var context = new GameBattingContext(new LineUpEntity(List.of(batter)));
        context.hitSingle(runner);

        // when
        var completed = new AtBatProcessor().process(context, batter);

        // then
        assertAll(
                () -> assertTrue(completed),
                () -> assertEquals(OutCount.ONE_OUT, context.getCurrentState().getOutCount()),
                () -> assertSame(runner, context.getCurrentState().runnerAt(Base.SECOND)),
                () -> assertEquals(1, context.getCurrentState().runnerCount()));
    }

    @Test
    @DisplayName("一死で積極的なバントが成功すると二死になり走者を進める")
    void appliesEagerBuntWithOneOut() {
        // given
        var runner = batter(0.0f, BehaviorStrategies.noBunt());
        var batter = batter(1.0f, BehaviorStrategies.eagerBunt());
        var context = new GameBattingContext(new LineUpEntity(List.of(batter)));
        context.out();
        context.hitSingle(runner);

        // when
        var completed = new AtBatProcessor().process(context, batter);

        // then
        assertAll(
                () -> assertTrue(completed),
                () -> assertEquals(OutCount.TWO_OUT, context.getCurrentState().getOutCount()),
                () -> assertSame(runner, context.getCurrentState().runnerAt(Base.SECOND)),
                () -> assertEquals(1, context.getCurrentState().runnerCount()));
    }

    private static BatterEntity batter(float buntSuccessRate, BuntStrategy buntStrategy) {
        return new BatterEntity(
                0.0f,
                0.0f,
                buntSuccessRate,
                0.0f,
                BehaviorStrategies.middleDistanceAtBat(),
                BehaviorStrategies.noSteal(),
                buntStrategy);
    }
}

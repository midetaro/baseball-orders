package com.example.baseballorders.simulator.domain.game;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.example.baseballorders.simulator.domain.play.OutCount;
import com.example.baseballorders.simulator.domain.player.BatterEntity;
import com.example.baseballorders.simulator.domain.player.LineUpEntity;
import com.example.baseballorders.simulator.domain.player.strategy.ScriptedRandom;
import com.example.baseballorders.simulator.domain.rule.RunnerAdvanceProbabilitiesBuilder;
import com.example.baseballorders.simulator.domain.rule.SimulationRulesTestData;
import com.example.baseballorders.simulator.domain.statistics.GameCompletionObserver;
import java.util.Collections;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 凡退時の進塁確率が、設定から供給された値で決まることを固定する。 */
class BattingOutAdvanceConfigurationTest {

    private static final GameCompletionObserver NO_OPERATION_OBSERVER = (score, statistics) -> {};

    @Test
    @DisplayName("進塁確率1.0の設定では凡退で一塁走者が二塁へ進む")
    void advancesRunnerWhenConfiguredProbabilityCoversRandom() {
        // given
        var sut = gameWithRunnerOnFirst(1.0f);
        var runner = sut.inningStateContext().currentBaseState().runnerAt(Base.FIRST);

        // when
        try (ScriptedRandom scriptedRandom = ScriptedRandom.of(0.5f)) {
            sut.inningStateContext().currentBaseState().battingOut();

            // then
            assertAll(
                    () -> assertEquals(1, scriptedRandom.consumedCount()),
                    scriptedRandom::assertFullyConsumed);
        }

        // then
        var after = sut.inningStateContext().currentBaseState();
        assertAll(
                () -> assertInstanceOf(DoubleBaseState.class, after),
                () -> assertNull(after.runnerAt(Base.FIRST)),
                () -> assertSame(runner, after.runnerAt(Base.SECOND)),
                () -> assertEquals(OutCount.ONE_OUT, after.getOutCount()));
    }

    @Test
    @DisplayName("進塁確率0.0の設定では同じ乱数でも一塁走者が動かない")
    void keepsRunnerWhenConfiguredProbabilityExcludesRandom() {
        // given
        var sut = gameWithRunnerOnFirst(0.0f);
        var runner = sut.inningStateContext().currentBaseState().runnerAt(Base.FIRST);

        // when
        try (ScriptedRandom scriptedRandom = ScriptedRandom.of(0.5f)) {
            sut.inningStateContext().currentBaseState().battingOut();

            // then
            assertAll(
                    () -> assertEquals(1, scriptedRandom.consumedCount()),
                    scriptedRandom::assertFullyConsumed);
        }

        // then
        var after = sut.inningStateContext().currentBaseState();
        assertAll(
                () -> assertInstanceOf(SingleBasesState.class, after),
                () -> assertSame(runner, after.runnerAt(Base.FIRST)),
                () -> assertNull(after.runnerAt(Base.SECOND)),
                () -> assertEquals(OutCount.ONE_OUT, after.getOutCount()));
    }

    private static GameBattingContext gameWithRunnerOnFirst(float fromFirstProbability) {
        var factory =
                new BaseStateFactory(
                        RunnerAdvanceProbabilitiesBuilder.runnerAdvanceProbabilities()
                                .fromFirstProbability(fromFirstProbability)
                                .fromSecondProbability(0.0f)
                                .fromThirdProbability(0.0f)
                                .build());
        var context =
                new GameBattingContext(
                        new LineUpEntity(Collections.nCopies(9, batter())),
                        NO_OPERATION_OBSERVER,
                        factory);
        context.inningStateContext().currentBaseState().hitSingle(batter());
        return context;
    }

    private static BatterEntity batter() {
        return new BatterEntity(
                0.3f,
                0.4f,
                0.0f,
                0.0f,
                SimulationRulesTestData.strategies().middleDistanceHittingStrategy(),
                SimulationRulesTestData.strategies().noSteal(),
                SimulationRulesTestData.strategies().noBunt());
    }
}

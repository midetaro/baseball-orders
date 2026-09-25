package com.example.baseballorders.simulator.domain.game;

import com.example.baseballorders.simulator.domain.game.capability.AdvancingBuntable;
import com.example.baseballorders.simulator.domain.game.capability.StealableToTripleBase;
import com.example.baseballorders.simulator.domain.play.BuntResult;
import com.example.baseballorders.simulator.domain.play.BuntType;
import com.example.baseballorders.simulator.domain.play.StealResult;
import com.example.baseballorders.simulator.domain.player.BatterEntity;
import com.example.baseballorders.simulator.domain.rule.RunnerAdvanceProbabilities;

/** 走者配置3のイベントを処理する試合固有のState。 */
public final class FirstDoubleBaseState extends AbstractBasesState
        implements BasesState, StealableToTripleBase, AdvancingBuntable {
    FirstDoubleBaseState(
            InningStateContext context, RunnerAdvanceProbabilities runnerAdvanceProbabilities) {
        super(context, runnerAdvanceProbabilities);
    }

    @Override
    public void walk(BatterEntity batter) {
        transition(batter, runnerAt(Base.FIRST), runnerAt(Base.SECOND), 0);
    }

    @Override
    public BuntResult bunt(BatterEntity batter) {
        return attemptBunt(batter, BuntType.ADVANCING);
    }

    @Override
    public void hitSingle(BatterEntity batter) {
        transition(batter, runnerAt(Base.FIRST), runnerAt(Base.SECOND), 0);
    }

    @Override
    public void hitDouble(BatterEntity batter) {
        transition(null, batter, runnerAt(Base.FIRST), 1);
    }

    @Override
    public void hitTriple(BatterEntity batter) {
        applyHitTriple(batter);
    }

    @Override
    public void hitHomer() {
        applyHitHomer();
    }

    @Override
    public StealResult stealToDouble() {
        return attemptStealToDouble();
    }

    @Override
    public StealResult stealToTriple() {
        return attemptStealToTriple();
    }

    @Override
    public BatterEntity runnerOnSecond() {
        return runnerAt(Base.SECOND);
    }
}

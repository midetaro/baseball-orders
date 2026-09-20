package com.example.baseballorders.simulator.domain.model.base;

import com.example.baseballorders.simulator.domain.code.Base;
import com.example.baseballorders.simulator.domain.code.BuntResult;
import com.example.baseballorders.simulator.domain.code.StealResult;
import com.example.baseballorders.simulator.domain.entity.player.BatterEntity;
import com.example.baseballorders.simulator.domain.model.GameBattingContext;
import com.example.baseballorders.simulator.domain.model.base.capability.AdvancingBuntable;
import com.example.baseballorders.simulator.domain.model.base.capability.StealableToTripleBase;

/** 走者配置3のイベントを処理する試合固有のState。 */
public final class FirstDoubleBaseState extends AbstractBasesState
        implements BasesState, StealableToTripleBase, AdvancingBuntable {
    FirstDoubleBaseState(GameBattingContext context, InningState inningState) {
        super(context, inningState);
    }

    @Override
    public BuntResult bunt(BatterEntity batter) {
        return attemptBunt(batter);
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

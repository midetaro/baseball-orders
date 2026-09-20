package com.example.baseballorders.simulator.domain.model.base;

import com.example.baseballorders.simulator.domain.code.Base;
import com.example.baseballorders.simulator.domain.code.BuntResult;
import com.example.baseballorders.simulator.domain.code.StealResult;
import com.example.baseballorders.simulator.domain.entity.player.BatterEntity;
import com.example.baseballorders.simulator.domain.model.GameBattingContext;
import com.example.baseballorders.simulator.domain.model.base.capability.AdvancingBuntable;
import com.example.baseballorders.simulator.domain.model.base.capability.StealableToDoubleBase;

/** 走者配置1のイベントを処理する試合固有のState。 */
public final class SingleBasesState extends AbstractBasesState
        implements BasesState, StealableToDoubleBase, AdvancingBuntable {
    SingleBasesState(GameBattingContext context, InningState inningState) {
        super(context, inningState);
    }

    @Override
    public BuntResult bunt(BatterEntity batter) {
        return attemptBunt(batter);
    }

    @Override
    public void hitSingle(BatterEntity batter) {
        transition(batter, runnerAt(Base.FIRST), null, 0);
    }

    @Override
    public void hitDouble(BatterEntity batter) {
        transition(null, batter, runnerAt(Base.FIRST), 0);
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
    public BatterEntity runnerOnFirst() {
        return runnerAt(Base.FIRST);
    }
}

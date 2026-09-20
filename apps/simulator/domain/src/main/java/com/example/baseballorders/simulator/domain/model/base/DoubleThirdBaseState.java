package com.example.baseballorders.simulator.domain.model.base;

import com.example.baseballorders.simulator.domain.code.Base;
import com.example.baseballorders.simulator.domain.code.BuntResult;
import com.example.baseballorders.simulator.domain.code.StealResult;
import com.example.baseballorders.simulator.domain.entity.player.BatterEntity;
import com.example.baseballorders.simulator.domain.model.GameBattingContext;
import com.example.baseballorders.simulator.domain.model.base.capability.SqueezeBuntable;

/** 走者配置6のイベントを処理する試合固有のState。 */
public final class DoubleThirdBaseState extends AbstractBasesState implements BasesState, SqueezeBuntable {
    DoubleThirdBaseState(GameBattingContext context, InningState inningState) {
        super(context, inningState);
    }

    @Override
    public BuntResult bunt(BatterEntity batter) {
        return attemptBunt(this, batter);
    }

    @Override
    public void hitSingle(BatterEntity batter) {
        transition(batter, null, runnerAt(Base.SECOND), 1);
    }

    @Override
    public void hitDouble(BatterEntity batter) {
        transition(null, batter, null, 2);
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
}

package com.example.baseballorders.simulator.domain.model.base;

import com.example.baseballorders.simulator.domain.code.BuntResult;
import com.example.baseballorders.simulator.domain.entity.player.BatterEntity;
import com.example.baseballorders.simulator.domain.model.GameBattingContext;
import com.example.baseballorders.simulator.domain.model.base.capability.SqueezeBuntable;

/** 走者配置4のイベントを処理する試合固有のState。 */
public final class ThirdBaseState extends AbstractBasesState
        implements BasesState, SqueezeBuntable {
    ThirdBaseState(GameBattingContext context, InningState inningState) {
        super(context, inningState);
    }

    @Override
    public BuntResult bunt(BatterEntity batter) {
        return attemptBunt(this, batter);
    }

    @Override
    public void hitSingle(BatterEntity batter) {
        transition(batter, null, null, 1);
    }

    @Override
    public void hitDouble(BatterEntity batter) {
        transition(null, batter, null, 1);
    }

    @Override
    public void hitTriple(BatterEntity batter) {
        applyHitTriple(batter);
    }

    @Override
    public void hitHomer() {
        applyHitHomer();
    }
}

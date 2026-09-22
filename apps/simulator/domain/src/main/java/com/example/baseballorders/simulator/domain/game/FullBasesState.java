package com.example.baseballorders.simulator.domain.game;

import com.example.baseballorders.simulator.domain.game.capability.SqueezeBuntable;
import com.example.baseballorders.simulator.domain.play.BuntResult;
import com.example.baseballorders.simulator.domain.play.BuntType;
import com.example.baseballorders.simulator.domain.player.BatterEntity;

/** 走者配置7のイベントを処理する試合固有のState。 */
public final class FullBasesState extends AbstractBasesState
        implements BasesState, SqueezeBuntable {
    FullBasesState(InningStateContext context) {
        super(context);
    }

    @Override
    public void walk(BatterEntity batter) {
        transition(batter, runnerAt(Base.FIRST), runnerAt(Base.SECOND), 1);
    }

    @Override
    public BuntResult bunt(BatterEntity batter) {
        return attemptBunt(batter, BuntType.SQUEEZE);
    }

    @Override
    public void hitSingle(BatterEntity batter) {
        transition(batter, runnerAt(Base.FIRST), runnerAt(Base.SECOND), 1);
    }

    @Override
    public void hitDouble(BatterEntity batter) {
        transition(null, batter, runnerAt(Base.FIRST), 2);
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

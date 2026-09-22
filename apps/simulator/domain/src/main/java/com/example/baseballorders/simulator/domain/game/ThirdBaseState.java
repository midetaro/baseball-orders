package com.example.baseballorders.simulator.domain.game;

import com.example.baseballorders.simulator.domain.game.capability.SqueezeBuntable;
import com.example.baseballorders.simulator.domain.play.BuntResult;
import com.example.baseballorders.simulator.domain.play.BuntType;
import com.example.baseballorders.simulator.domain.player.BatterEntity;

/** 走者配置4のイベントを処理する試合固有のState。 */
public final class ThirdBaseState extends AbstractBasesState
        implements BasesState, SqueezeBuntable {
    ThirdBaseState(GameBattingContext context, InningState inningState) {
        super(context, inningState);
    }

    @Override
    public void walk(BatterEntity batter) {
        transition(batter, null, runnerAt(Base.THIRD), 0);
    }

    @Override
    public BuntResult bunt(BatterEntity batter) {
        return attemptBunt(batter, BuntType.SQUEEZE);
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

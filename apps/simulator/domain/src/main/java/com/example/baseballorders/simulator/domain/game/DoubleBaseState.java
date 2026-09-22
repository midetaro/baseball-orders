package com.example.baseballorders.simulator.domain.game;

import com.example.baseballorders.simulator.domain.game.capability.AdvancingBuntable;
import com.example.baseballorders.simulator.domain.game.capability.StealableToTripleBase;
import com.example.baseballorders.simulator.domain.play.BuntResult;
import com.example.baseballorders.simulator.domain.play.BuntType;
import com.example.baseballorders.simulator.domain.play.StealResult;
import com.example.baseballorders.simulator.domain.player.BatterEntity;

/** 走者配置2のイベントを処理する試合固有のState。 */
public final class DoubleBaseState extends AbstractBasesState
        implements BasesState, StealableToTripleBase, AdvancingBuntable {
    DoubleBaseState(GameBattingContext context, InningState inningState) {
        super(context, inningState);
    }

    @Override
    public void walk(BatterEntity batter) {
        transition(batter, runnerAt(Base.SECOND), null, 0);
    }

    @Override
    public BuntResult bunt(BatterEntity batter) {
        return attemptBunt(batter, BuntType.ADVANCING);
    }

    @Override
    public void hitSingle(BatterEntity batter) {
        transition(batter, null, runnerAt(Base.SECOND), 0);
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

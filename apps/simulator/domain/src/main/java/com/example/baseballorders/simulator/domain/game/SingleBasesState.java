package com.example.baseballorders.simulator.domain.game;

import com.example.baseballorders.simulator.domain.game.capability.AdvancingBuntable;
import com.example.baseballorders.simulator.domain.game.capability.StealableToDoubleBase;
import com.example.baseballorders.simulator.domain.play.BuntResult;
import com.example.baseballorders.simulator.domain.play.BuntType;
import com.example.baseballorders.simulator.domain.play.StealResult;
import com.example.baseballorders.simulator.domain.player.BatterEntity;

/** 走者配置1のイベントを処理する試合固有のState。 */
public final class SingleBasesState extends AbstractBasesState
        implements BasesState, StealableToDoubleBase, AdvancingBuntable {
    SingleBasesState(InningStateContext context) {
        super(context);
    }

    @Override
    public void walk(BatterEntity batter) {
        transition(batter, runnerAt(Base.FIRST), null, 0);
    }

    @Override
    public BuntResult bunt(BatterEntity batter) {
        return attemptBunt(batter, BuntType.ADVANCING);
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

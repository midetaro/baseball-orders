package com.example.baseballorders.simulator.domain.game;

import com.example.baseballorders.simulator.domain.player.BatterEntity;
import com.example.baseballorders.simulator.domain.rule.RunnerAdvanceProbabilities;

/** 走者配置0のイベントを処理する試合固有のState。 */
public final class NoBasesState extends AbstractBasesState implements BasesState {
    NoBasesState(
            InningStateContext context, RunnerAdvanceProbabilities runnerAdvanceProbabilities) {
        super(context, runnerAdvanceProbabilities);
    }

    @Override
    public void walk(BatterEntity batter) {
        transition(batter, null, null, 0);
    }

    @Override
    public void hitSingle(BatterEntity batter) {
        transition(batter, null, null, 0);
    }

    @Override
    public void hitDouble(BatterEntity batter) {
        transition(null, batter, null, 0);
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

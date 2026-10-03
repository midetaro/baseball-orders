package com.example.baseballorders.simulator.domain.player.strategy.steal;

import com.example.baseballorders.simulator.domain.play.StealResult;

public final class NowayStealStrategy implements StealStrategy {

    @Override
    public StealResult runToDouble() {
        return StealResult.NOT_TRY;
    }

    @Override
    public StealResult runToTriple() {
        return StealResult.NOT_TRY;
    }
}

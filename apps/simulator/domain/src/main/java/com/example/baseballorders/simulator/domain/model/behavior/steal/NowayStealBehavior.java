package com.example.baseballorders.simulator.domain.model.behavior.steal;

import com.example.baseballorders.simulator.domain.code.StealResult;

public final class NowayStealBehavior implements StealStrategy {

    @Override
    public StealResult runToDouble(float successRate) {
        return StealResult.NOT_TRY;
    }

    @Override
    public StealResult runToTriple(float successRate) {
        return StealResult.NOT_TRY;
    }
}

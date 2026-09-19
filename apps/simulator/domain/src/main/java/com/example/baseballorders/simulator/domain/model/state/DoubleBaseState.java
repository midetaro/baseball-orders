package com.example.baseballorders.simulator.domain.model.state;

import com.example.baseballorders.simulator.domain.model.player.BatterEntity;

public final class DoubleBaseState extends BasesState
        implements StealableToTripleBase, AdvancingBuntable {

    public DoubleBaseState(BatterEntity secondRunner) {
        super(null, secondRunner, null);
    }

    @Override
    public BatterEntity runnerOnSecond() {
        return runnerAt(com.example.baseballorders.simulator.domain.code.Base.SECOND);
    }
}

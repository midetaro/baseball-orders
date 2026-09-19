package com.example.baseballorders.simulator.domain.model.state;

import com.example.baseballorders.simulator.domain.model.player.BatterEntity;

public final class FirstDoubleBaseState extends BasesState
        implements StealableToTripleBase, Buntable {

    public FirstDoubleBaseState(BatterEntity firstRunner, BatterEntity secondRunner) {
        super(firstRunner, secondRunner, null);
    }

    @Override
    public BatterEntity runnerOnSecond() {
        return runnerAt(com.example.baseballorders.simulator.domain.code.Base.SECOND);
    }
}

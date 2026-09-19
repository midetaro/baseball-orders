package com.example.baseballorders.simulator.domain.model.state;

import com.example.baseballorders.simulator.domain.model.player.BatterEntity;

public final class SingleBasesState extends BasesState
        implements StealableToDoubleBase, AdvancingBuntable {

    public SingleBasesState(BatterEntity firstRunner) {
        super(firstRunner, null, null);
    }

    @Override
    public BatterEntity runnerOnFirst() {
        return runnerAt(com.example.baseballorders.simulator.domain.code.Base.FIRST);
    }
}

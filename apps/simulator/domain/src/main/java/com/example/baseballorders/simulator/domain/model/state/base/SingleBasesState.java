package com.example.baseballorders.simulator.domain.model.state.base;

import com.example.baseballorders.simulator.domain.entity.player.BatterEntity;
import com.example.baseballorders.simulator.domain.model.state.transaction.AdvancingBuntable;
import com.example.baseballorders.simulator.domain.model.state.BasesState;
import com.example.baseballorders.simulator.domain.model.state.transaction.StealableToDoubleBase;

public final class SingleBasesState extends BasesState
        implements StealableToDoubleBase, AdvancingBuntable {

    public SingleBasesState(BatterEntity firstRunner) {
        super(firstRunner, null, null);
    }

    SingleBasesState(BaseStateFactory factory, BatterEntity firstRunner) {
        super(factory, firstRunner, null, null);
    }

    @Override
    public BatterEntity runnerOnFirst() {
        return runnerAt(com.example.baseballorders.simulator.domain.code.Base.FIRST);
    }
}

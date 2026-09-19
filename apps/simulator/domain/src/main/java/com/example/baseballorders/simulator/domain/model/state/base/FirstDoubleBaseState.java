package com.example.baseballorders.simulator.domain.model.state.base;

import com.example.baseballorders.simulator.domain.entity.player.BatterEntity;
import com.example.baseballorders.simulator.domain.model.state.AdvancingBuntable;
import com.example.baseballorders.simulator.domain.model.state.BasesState;
import com.example.baseballorders.simulator.domain.model.state.StealableToTripleBase;

public final class FirstDoubleBaseState extends BasesState
        implements StealableToTripleBase, AdvancingBuntable {

    public FirstDoubleBaseState(BatterEntity firstRunner, BatterEntity secondRunner) {
        super(firstRunner, secondRunner, null);
    }

    FirstDoubleBaseState(
            BaseStateFactory factory, BatterEntity firstRunner, BatterEntity secondRunner) {
        super(factory, firstRunner, secondRunner, null);
    }

    @Override
    public BatterEntity runnerOnSecond() {
        return runnerAt(com.example.baseballorders.simulator.domain.code.Base.SECOND);
    }
}

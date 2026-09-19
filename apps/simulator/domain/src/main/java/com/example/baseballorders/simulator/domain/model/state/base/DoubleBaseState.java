package com.example.baseballorders.simulator.domain.model.state.base;

import com.example.baseballorders.simulator.domain.entity.player.BatterEntity;
import com.example.baseballorders.simulator.domain.model.state.AdvancingBuntable;
import com.example.baseballorders.simulator.domain.model.state.BasesState;
import com.example.baseballorders.simulator.domain.model.state.StealableToTripleBase;

public final class DoubleBaseState extends BasesState
        implements StealableToTripleBase, AdvancingBuntable {

    public DoubleBaseState(BatterEntity secondRunner) {
        super(null, secondRunner, null);
    }

    DoubleBaseState(BaseStateFactory factory, BatterEntity secondRunner) {
        super(factory, null, secondRunner, null);
    }

    @Override
    public BatterEntity runnerOnSecond() {
        return runnerAt(com.example.baseballorders.simulator.domain.code.Base.SECOND);
    }
}

package com.example.baseballorders.simulator.domain.model.state.base;

import com.example.baseballorders.simulator.domain.entity.player.BatterEntity;
import com.example.baseballorders.simulator.domain.model.state.BasesState;
import com.example.baseballorders.simulator.domain.model.state.StealableToDoubleBase;

public final class FirstThirdBaseState extends BasesState implements StealableToDoubleBase {

    public FirstThirdBaseState(BatterEntity firstRunner, BatterEntity thirdRunner) {
        super(firstRunner, null, thirdRunner);
    }

    FirstThirdBaseState(
            BaseStateFactory factory, BatterEntity firstRunner, BatterEntity thirdRunner) {
        super(factory, firstRunner, null, thirdRunner);
    }

    @Override
    public BatterEntity runnerOnFirst() {
        return runnerAt(com.example.baseballorders.simulator.domain.code.Base.FIRST);
    }
}

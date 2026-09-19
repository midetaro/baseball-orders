package com.example.baseballorders.simulator.domain.model.state;

import com.example.baseballorders.simulator.domain.model.player.BatterEntity;

public final class FirstThirdBaseState extends BasesState implements StealableToDoubleBase {

    public FirstThirdBaseState(BatterEntity firstRunner, BatterEntity thirdRunner) {
        super(firstRunner, null, thirdRunner);
    }

    @Override
    public BatterEntity runnerOnFirst() {
        return runnerAt(com.example.baseballorders.simulator.domain.code.Base.FIRST);
    }
}

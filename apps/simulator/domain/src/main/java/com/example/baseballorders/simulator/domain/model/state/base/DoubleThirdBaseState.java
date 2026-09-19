package com.example.baseballorders.simulator.domain.model.state.base;

import com.example.baseballorders.simulator.domain.entity.player.BatterEntity;
import com.example.baseballorders.simulator.domain.model.state.BasesState;

public final class DoubleThirdBaseState extends BasesState {

    public DoubleThirdBaseState(BatterEntity secondRunner, BatterEntity thirdRunner) {
        super(null, secondRunner, thirdRunner);
    }

    DoubleThirdBaseState(
            BaseStateFactory factory, BatterEntity secondRunner, BatterEntity thirdRunner) {
        super(factory, null, secondRunner, thirdRunner);
    }
}

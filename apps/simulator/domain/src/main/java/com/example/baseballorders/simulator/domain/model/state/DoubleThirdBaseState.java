package com.example.baseballorders.simulator.domain.model.state;

import com.example.baseballorders.simulator.domain.model.player.BatterEntity;

public final class DoubleThirdBaseState extends BasesState {

    public DoubleThirdBaseState(BatterEntity secondRunner, BatterEntity thirdRunner) {
        super(null, secondRunner, thirdRunner);
    }
}

package com.example.baseballorders.simulator.domain.model.state;

import com.example.baseballorders.simulator.domain.model.player.BatterEntity;

public final class ThirdBaseState extends BasesState {

    public ThirdBaseState(BatterEntity thirdRunner) {
        super(null, null, thirdRunner);
    }
}

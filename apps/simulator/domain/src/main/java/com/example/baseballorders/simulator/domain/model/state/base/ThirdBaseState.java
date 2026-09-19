package com.example.baseballorders.simulator.domain.model.state.base;

import com.example.baseballorders.simulator.domain.entity.player.BatterEntity;
import com.example.baseballorders.simulator.domain.model.state.BasesState;

public final class ThirdBaseState extends BasesState {

    public ThirdBaseState(BatterEntity thirdRunner) {
        super(null, null, thirdRunner);
    }

    ThirdBaseState(BaseStateFactory factory, BatterEntity thirdRunner) {
        super(factory, null, null, thirdRunner);
    }
}

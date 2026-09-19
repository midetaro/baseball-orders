package com.example.baseballorders.simulator.domain.model.state.base;

import com.example.baseballorders.simulator.domain.entity.player.BatterEntity;
import com.example.baseballorders.simulator.domain.model.state.BasesState;

public final class FullBasesState extends BasesState {

    public FullBasesState(
            BatterEntity firstRunner, BatterEntity secondRunner, BatterEntity thirdRunner) {
        super(firstRunner, secondRunner, thirdRunner);
    }

    FullBasesState(
            BaseStateFactory factory,
            BatterEntity firstRunner,
            BatterEntity secondRunner,
            BatterEntity thirdRunner) {
        super(factory, firstRunner, secondRunner, thirdRunner);
    }
}

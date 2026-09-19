package com.example.baseballorders.simulator.domain.model.state;

import com.example.baseballorders.simulator.domain.model.player.BatterEntity;

public final class FullBasesState extends BasesState {

    public FullBasesState(
            BatterEntity firstRunner, BatterEntity secondRunner, BatterEntity thirdRunner) {
        super(firstRunner, secondRunner, thirdRunner);
    }
}

package com.example.baseballorders.simulator.domain.model.state.base;

import com.example.baseballorders.simulator.domain.model.state.BasesState;

public final class NoBasesState extends BasesState {
    public NoBasesState() {
        super(null, null, null);
    }

    NoBasesState(BaseStateFactory factory) {
        super(factory, null, null, null);
    }
}

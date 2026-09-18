package com.example.baseballorders.simulator.domain.model;

import com.example.baseballorders.simulator.domain.model.state.BasesState;
import com.example.baseballorders.simulator.domain.model.state.DoubleBaseState;
import com.example.baseballorders.simulator.domain.model.state.DoubleThirdBaseState;
import com.example.baseballorders.simulator.domain.model.state.FirstDoubleBaseState;
import com.example.baseballorders.simulator.domain.model.state.FirstThirdBaseState;
import com.example.baseballorders.simulator.domain.model.state.FullBasesState;
import com.example.baseballorders.simulator.domain.model.state.NoBasesState;
import com.example.baseballorders.simulator.domain.model.state.SingleBasesState;
import com.example.baseballorders.simulator.domain.model.state.ThirdBaseState;

/** 走者配置に対応する塁状態を選択する Factory。 */
final class BasesStateResolver {

    private BasesStateResolver() {}

    static BasesState resolve(BaseRunners runners) {
        if (runners.getFirst() != null
                && runners.getSecond() != null
                && runners.getThird() != null) {
            return new FullBasesState();
        }
        if (runners.getFirst() != null && runners.getSecond() != null) {
            return new FirstDoubleBaseState();
        }
        if (runners.getFirst() != null && runners.getThird() != null) {
            return new FirstThirdBaseState();
        }
        if (runners.getFirst() != null) {
            return new SingleBasesState();
        }
        if (runners.getSecond() != null && runners.getThird() != null) {
            return new DoubleThirdBaseState();
        }
        if (runners.getSecond() != null) {
            return new DoubleBaseState();
        }
        if (runners.getThird() != null) {
            return new ThirdBaseState();
        }
        return new NoBasesState();
    }
}

package com.example.baseballorders.simulator.domain.entity.behavior.bunt;

import com.example.baseballorders.simulator.domain.code.BuntResult;
import com.example.baseballorders.simulator.domain.code.OutCount;
import com.example.baseballorders.simulator.domain.model.state.BasesState;

/** A bunt strategy that never attempts a bunt. */
public final class NowayBuntStrategy implements BuntStrategy {

    @Override
    public BuntResult bunt(float successRate, OutCount outCount, BasesState basesState) {
        return BuntResult.NOT_TRY;
    }
}

package com.example.baseballorders.simulator.domain.entity.behavior.bunt;

import com.example.baseballorders.simulator.domain.code.BuntResult;
import com.example.baseballorders.simulator.domain.model.base.BasesState;

/** A bunt strategy that never attempts a bunt. */
public final class NowayBuntStrategy implements BuntStrategy {

    @Override
    public BuntResult bunt(float successRate, BasesState basesState) {
        return BuntResult.NOT_TRY;
    }
}

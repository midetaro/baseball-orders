package com.example.baseballorders.simulator.domain.player.strategy.bunt;

import com.example.baseballorders.simulator.domain.play.BuntResult;
import com.example.baseballorders.simulator.domain.play.BuntType;
import com.example.baseballorders.simulator.domain.play.OutCount;

/** A bunt strategy that never attempts a bunt. */
public final class NowayBuntStrategy implements BuntStrategy {

    @Override
    public BuntResult bunt(OutCount outCount, BuntType buntType) {
        return BuntResult.NOT_TRY;
    }
}

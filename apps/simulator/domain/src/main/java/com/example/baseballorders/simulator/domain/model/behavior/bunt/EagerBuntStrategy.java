package com.example.baseballorders.simulator.domain.model.behavior.bunt;

import com.example.baseballorders.simulator.domain.code.BuntResult;
import com.example.baseballorders.simulator.domain.code.OutCount;
import com.example.baseballorders.simulator.domain.model.state.BasesState;
import com.example.baseballorders.simulator.domain.util.RandomGenerator;

/** 標準戦略より広い試合状況でバントを試みる積極的な戦略。 */
public final class EagerBuntStrategy implements BuntStrategy {

    @Override
    public BuntResult bunt(float successRate, OutCount outCount, BasesState basesState) {
        return switch (outCount) {
            case NO_OUT, ONE_OUT -> attempt(successRate);
            case TWO_OUT, THREE_OUT -> BuntResult.NOT_TRY;
        };
    }

    private BuntResult attempt(float successRate) {
        return RandomGenerator.nextFloat() < successRate ? BuntResult.SUCCESS : BuntResult.FAILURE;
    }
}

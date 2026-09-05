package com.example.baseballorders.simulator.domain.model.behavior;

import com.example.baseballorders.simulator.domain.code.BuntResult;
import com.example.baseballorders.simulator.domain.code.OutCount;
import com.example.baseballorders.simulator.domain.model.state.BasesState;
import com.example.baseballorders.simulator.domain.model.state.DoubleBaseState;
import com.example.baseballorders.simulator.domain.model.state.FirstDoubleBaseState;
import com.example.baseballorders.simulator.domain.model.state.SingleBasesState;
import com.example.baseballorders.simulator.domain.util.RandomGenerator;

/** 標準戦略より広い試合状況でバントを試みる積極的な戦略。 */
public class EagerBuntStrategy implements BuntStrategy {

    @Override
    public BuntResult bunt(float successRate, OutCount outCount, BasesState basesState) {
        return switch (outCount) {
            case NO_OUT -> buntWithNoOut(successRate, basesState);
            case ONE_OUT -> buntWithOneOut(successRate, basesState);
            case TWO_OUT, THREE_OUT -> BuntResult.NOT_TRY;
        };
    }

    private BuntResult buntWithNoOut(float successRate, BasesState basesState) {
        if (basesState instanceof SingleBasesState) {
            return attempt(successRate);
        }
        if (basesState instanceof FirstDoubleBaseState) {
            return attempt(successRate);
        }
        if (basesState instanceof DoubleBaseState) {
            return attempt(successRate);
        }
        return BuntResult.NOT_TRY;
    }

    private BuntResult buntWithOneOut(float successRate, BasesState basesState) {
        return basesState instanceof SingleBasesState ? attempt(successRate) : BuntResult.NOT_TRY;
    }

    private BuntResult attempt(float successRate) {
        return RandomGenerator.nextFloat() < successRate ? BuntResult.SUCCESS : BuntResult.FAILURE;
    }
}

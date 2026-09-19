package com.example.baseballorders.simulator.domain.model.behavior;

import com.example.baseballorders.simulator.domain.code.Base;
import com.example.baseballorders.simulator.domain.code.BuntResult;
import com.example.baseballorders.simulator.domain.code.OutCount;
import com.example.baseballorders.simulator.domain.model.state.BasesState;
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
        return basesState.buntOpportunity().isPresent() ? attempt(successRate) : BuntResult.NOT_TRY;
    }

    private BuntResult buntWithOneOut(float successRate, BasesState basesState) {
        return basesState.buntOpportunity().isPresent()
                        && basesState.isOccupied(Base.FIRST)
                        && !basesState.isOccupied(Base.SECOND)
                ? attempt(successRate)
                : BuntResult.NOT_TRY;
    }

    private BuntResult attempt(float successRate) {
        return RandomGenerator.nextFloat() < successRate ? BuntResult.SUCCESS : BuntResult.FAILURE;
    }
}

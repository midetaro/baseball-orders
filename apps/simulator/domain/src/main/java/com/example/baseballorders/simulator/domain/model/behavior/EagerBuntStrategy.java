package com.example.baseballorders.simulator.domain.model.behavior;

import com.example.baseballorders.simulator.domain.code.BuntResult;
import com.example.baseballorders.simulator.domain.model.state.BasesState;
import com.example.baseballorders.simulator.domain.model.state.DoubleBaseState;
import com.example.baseballorders.simulator.domain.model.state.FirstDoubleBaseState;
import com.example.baseballorders.simulator.domain.model.state.SingleBasesState;
import com.example.baseballorders.simulator.domain.util.RandomGenerator;
import org.springframework.stereotype.Component;

/** 標準戦略より広い試合状況でバントを試みる積極的な戦略。 */
@Component("eagerBuntStrategy")
public class EagerBuntStrategy implements BuntStrategy {

    @Override
    public BuntResult bunt(float successRate, long outCounts, BasesState basesState) {
        if (outCounts == 0) {
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
        if (outCounts == 1 && basesState instanceof SingleBasesState) {
            return attempt(successRate);
        }
        return BuntResult.NOT_TRY;
    }

    private BuntResult attempt(float successRate) {
        return RandomGenerator.nextFloat() < successRate ? BuntResult.SUCCESS : BuntResult.FAILURE;
    }
}

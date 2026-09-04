package com.example.baseballorders.simulator.domain.model.behavior;

import com.example.baseballorders.simulator.domain.code.BuntResult;
import com.example.baseballorders.simulator.domain.model.state.BasesState;
import com.example.baseballorders.simulator.domain.model.state.FirstDoubleBaseState;
import com.example.baseballorders.simulator.domain.model.state.SingleBasesState;
import com.example.baseballorders.simulator.domain.util.RandomGenerator;
import org.springframework.stereotype.Component;

/** 選手のバント成功率に基づく標準的なバント戦略。 */
@Component("standardBuntStrategy")
public class StandardBuntStrategy implements BuntStrategy {

    @Override
    public BuntResult bunt(float successRate, long outCounts, BasesState basesState) {
        if (outCounts > 0) {
            return BuntResult.NOT_TRY;
        }
        if (basesState instanceof SingleBasesState) {
            return attempt(successRate);
        }
        if (basesState instanceof FirstDoubleBaseState) {
            return attempt(successRate);
        }
        return BuntResult.NOT_TRY;
    }

    private BuntResult attempt(float successRate) {
        return RandomGenerator.nextFloat() < successRate ? BuntResult.SUCCESS : BuntResult.FAILURE;
    }
}

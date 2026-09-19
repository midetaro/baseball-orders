package com.example.baseballorders.simulator.domain.model.behavior;

import com.example.baseballorders.simulator.domain.code.Base;
import com.example.baseballorders.simulator.domain.code.BuntResult;
import com.example.baseballorders.simulator.domain.code.OutCount;
import com.example.baseballorders.simulator.domain.model.state.BasesState;
import com.example.baseballorders.simulator.domain.util.RandomGenerator;
import org.springframework.stereotype.Component;

/** 選手のバント成功率に基づく標準的なバント戦略。 */
@Component("standardBuntStrategy")
public class StandardBuntStrategy implements BuntStrategy {

    @Override
    public BuntResult bunt(float successRate, OutCount outCount, BasesState basesState) {
        return switch (outCount) {
            case NO_OUT -> buntWithRunners(successRate, basesState);
            case ONE_OUT, TWO_OUT, THREE_OUT -> BuntResult.NOT_TRY;
        };
    }

    private BuntResult buntWithRunners(float successRate, BasesState basesState) {
        return basesState.buntOpportunity().isPresent() && basesState.isOccupied(Base.FIRST)
                ? attempt(successRate)
                : BuntResult.NOT_TRY;
    }

    private BuntResult attempt(float successRate) {
        return RandomGenerator.nextFloat() < successRate ? BuntResult.SUCCESS : BuntResult.FAILURE;
    }
}

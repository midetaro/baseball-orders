package com.example.baseballorders.simulator.domain.entity.behavior.bunt;

import com.example.baseballorders.simulator.domain.code.BuntResult;
import com.example.baseballorders.simulator.domain.code.OutCount;
import com.example.baseballorders.simulator.domain.util.RandomGenerator;
import org.springframework.stereotype.Component;

/** 選手のバント成功率に基づく標準的なバント戦略。 */
@Component("standardBuntStrategy")
public final class StandardBuntStrategy implements BuntStrategy {

    @Override
    public BuntResult bunt(float successRate, OutCount outCount) {
        return switch (outCount) {
            case NO_OUT -> attempt(successRate);
            case ONE_OUT, TWO_OUT, THREE_OUT -> BuntResult.NOT_TRY;
        };
    }

    private BuntResult attempt(float successRate) {
        return RandomGenerator.nextFloat() < successRate ? BuntResult.SUCCESS : BuntResult.FAILURE;
    }
}

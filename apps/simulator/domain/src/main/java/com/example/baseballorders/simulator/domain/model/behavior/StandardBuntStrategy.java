package com.example.baseballorders.simulator.domain.model.behavior;

import com.example.baseballorders.simulator.domain.code.BuntResult;
import com.example.baseballorders.simulator.domain.util.RandomGenerator;
import org.springframework.stereotype.Component;

/** 選手のバント成功率に基づく標準的なバント戦略。 */
@Component("standardBuntStrategy")
public class StandardBuntStrategy implements BuntStrategy {

    @Override
    public BuntResult bunt(float successRate) {
        return RandomGenerator.nextFloat() < successRate ? BuntResult.SUCCESS : BuntResult.FAILURE;
    }
}

package com.example.baseballorders.simulator.domain.model.behavior;

import com.example.baseballorders.simulator.domain.code.StealResult;
import com.example.baseballorders.simulator.domain.util.RandomGenerator;
import org.springframework.stereotype.Component;

@Component("eagerStealBehavior")
public class EagerStealBehavior implements StealStrategy {

    private final float TO_DOUBLE_TRY_AVERAGE = 0.3f;
    private final float TO_DOUBLE_NOT_TRY = 1 - TO_DOUBLE_TRY_AVERAGE;

    @Override
    public StealResult runToDouble(float successRate) {
        float random = RandomGenerator.nextFloat();
        float successProbability = TO_DOUBLE_NOT_TRY + successRate * TO_DOUBLE_TRY_AVERAGE;
        if (random < TO_DOUBLE_NOT_TRY) {
            return StealResult.NOT_TRY;
        } else if (TO_DOUBLE_NOT_TRY < random && random < successProbability) {
            return StealResult.SUCCESS;
        } else {
            return StealResult.FAILURE;
        }
    }

    private final float TO_TRIPLE_TRY_AVERAGE = 0.15f;
    private final float TO_TRIPLE_NOT_TRY = 1 - TO_TRIPLE_TRY_AVERAGE;

    @Override
    public StealResult runToTriple(float successRate) {
        float random = RandomGenerator.nextFloat();
        float successProbability = TO_TRIPLE_NOT_TRY + successRate * TO_TRIPLE_TRY_AVERAGE;
        if (random < TO_TRIPLE_NOT_TRY) {
            return StealResult.NOT_TRY;
        } else if (TO_TRIPLE_NOT_TRY < random && random < successProbability) {
            return StealResult.SUCCESS;
        } else {
            return StealResult.FAILURE;
        }
    }
}

package com.example.baseballorders.simulator.domain.model.behavior;

import com.example.baseballorders.simulator.domain.code.StealResult;
import com.example.baseballorders.simulator.domain.util.RandomGenerator;

public class MiddleStealBehavior implements StealStrategy {

    private final float TRY_AVERAGE = 0.2f;
    private final float NOT_TRY = 1 - TRY_AVERAGE;

    @Override
    public StealResult runToDouble(float successRate) {
        float random = RandomGenerator.nextFloat();
        float successProbability = NOT_TRY + successRate * TRY_AVERAGE;

        if (random < NOT_TRY) {
            return StealResult.NOT_TRY;
        } else if (NOT_TRY < random && random < successProbability) {
            return StealResult.SUCCESS;
        } else {
            return StealResult.FAILURE;
        }
    }

    private final float TO_TRIPLE_TRY_AVERAGE = 0.05f;
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

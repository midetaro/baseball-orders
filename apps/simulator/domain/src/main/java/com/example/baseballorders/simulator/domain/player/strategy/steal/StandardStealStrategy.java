package com.example.baseballorders.simulator.domain.player.strategy.steal;

import com.example.baseballorders.simulator.domain.play.StealResult;
import com.example.baseballorders.simulator.domain.player.strategy.RandomGenerator;
import com.example.baseballorders.simulator.domain.rule.StealAttemptRates;
import lombok.RequiredArgsConstructor;

/** 選手の盗塁成功率に基づく標準的な盗塁戦略。 */
@RequiredArgsConstructor
public final class StandardStealStrategy implements StealStrategy {

    private final StealAttemptRates attemptRates;

    @Override
    public StealResult runToDouble(float successRate) {
        float random = RandomGenerator.nextFloat();
        float tryAverage = attemptRates.toDoubleAttemptRate();
        float notTry = 1 - tryAverage;
        float successProbability = notTry + successRate * tryAverage;

        if (random < notTry) {
            return StealResult.NOT_TRY;
        } else if (notTry < random && random < successProbability) {
            return StealResult.SUCCESS;
        } else {
            return StealResult.FAILURE;
        }
    }

    @Override
    public StealResult runToTriple(float successRate) {
        float random = RandomGenerator.nextFloat();
        float tryAverage = attemptRates.toTripleAttemptRate();
        float notTry = 1 - tryAverage;
        float successProbability = notTry + successRate * tryAverage;
        if (random < notTry) {
            return StealResult.NOT_TRY;
        } else if (notTry < random && random < successProbability) {
            return StealResult.SUCCESS;
        } else {
            return StealResult.FAILURE;
        }
    }
}

package com.example.baseballorders.simulator.domain.player.strategy.batting;

import com.example.baseballorders.simulator.domain.play.BattingResult;
import com.example.baseballorders.simulator.domain.rule.BattingProbabilities;
import lombok.RequiredArgsConstructor;

/** 設定された打席確率に従って、乱数と安打配分から打席結果を決める。 */
@RequiredArgsConstructor
final class BattingResultSelector {

    private final BattingProbabilities probabilities;

    BattingResult select(
            float random,
            float onBasePercentage,
            float singleWeight,
            float doubleWeight,
            float tripleWeight,
            float homeRunWeight) {
        float walkProbability = Math.min(probabilities.walkProbability(), onBasePercentage);
        if (random < walkProbability) {
            return BattingResult.WALK;
        }

        float remainingOnBaseProbability = onBasePercentage - walkProbability;
        float totalHitWeight = singleWeight + doubleWeight + tripleWeight + homeRunWeight;
        float cumulative = walkProbability;
        float[] weights = {singleWeight, doubleWeight, tripleWeight, homeRunWeight};
        BattingResult[] results = {
            BattingResult.HIT_SINGLE,
            BattingResult.HIT_DOUBLE,
            BattingResult.HIT_TRIPLE,
            BattingResult.HIT_HOMER
        };
        int lastPositiveWeight = lastPositiveWeight(weights);
        for (int index = 0; index < weights.length; index++) {
            if (weights[index] <= 0) {
                continue;
            }
            cumulative =
                    index == lastPositiveWeight
                            ? onBasePercentage
                            : cumulative
                                    + remainingOnBaseProbability * weights[index] / totalHitWeight;
            if (random < cumulative) {
                return results[index];
            }
        }

        float strikeoutThreshold =
                onBasePercentage
                        + (1 - onBasePercentage)
                                * probabilities.strikeoutProbabilityWhenNotOnBase();
        return random < strikeoutThreshold ? BattingResult.STRIKEOUT : BattingResult.BATTED_OUT;
    }

    private static int lastPositiveWeight(float[] weights) {
        int lastPositiveWeight = 0;
        for (int index = 0; index < weights.length; index++) {
            if (weights[index] > 0) {
                lastPositiveWeight = index;
            }
        }
        return lastPositiveWeight;
    }
}

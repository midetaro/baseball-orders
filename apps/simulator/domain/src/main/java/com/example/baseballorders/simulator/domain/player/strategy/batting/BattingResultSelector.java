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
            float battingAverage,
            float singleWeight,
            float doubleWeight,
            float tripleWeight,
            float homeRunWeight) {
        float walkProbability = probabilities.walkProbability();
        if (random < walkProbability) {
            return BattingResult.WALK;
        }

        // 四球は打数に含まれないため、安打確率は四球以外の打席に打率を掛ける。
        float hitProbability = (1 - walkProbability) * battingAverage;
        float onBaseProbability = walkProbability + hitProbability;
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
                            ? onBaseProbability
                            : cumulative + hitProbability * weights[index] / totalHitWeight;
            if (random < cumulative) {
                return results[index];
            }
        }

        float strikeoutThreshold =
                onBaseProbability
                        + (1 - onBaseProbability)
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

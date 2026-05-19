package org.example.domain.model.behavior;

import org.example.domain.code.BattingResult;

/**
 * 長距離バッター
 */
public class LongDistanceBattingBehavior implements AtBatBehavior {

    @Override
    public BattingResult batting(float hitAverage, float sluggish) {
        float random = (float) Math.random();
        float singleProbability = hitAverage * (1 - sluggish);
        float doubleProbability = hitAverage * (sluggish / 2);
        float homerProbability = hitAverage * (sluggish / 2);

        if (random < singleProbability) {
            return BattingResult.HIT_SINGLE;
        } else if (random < singleProbability + doubleProbability) {
            return BattingResult.HIT_DOUBLE;
        } else if (random < singleProbability + doubleProbability + homerProbability) {
            return BattingResult.HIT_HOMER;
        }
        return BattingResult.OUT;
    }
}

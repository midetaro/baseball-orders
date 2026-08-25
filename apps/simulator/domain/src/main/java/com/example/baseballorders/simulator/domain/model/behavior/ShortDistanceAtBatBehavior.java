package com.example.baseballorders.simulator.domain.model.behavior;

import com.example.baseballorders.simulator.domain.code.BattingResult;
import org.springframework.stereotype.Component;

/**
 * 短距離バッター
 */
@Component("shortDistanceAtBat")
public class ShortDistanceAtBatBehavior implements AtBatBehavior {

    @Override
    public BattingResult batting(float hitAverage, float sluggish) {

        float random = (float) Math.random();
        float singleProbability = hitAverage * (1 - sluggish);
        float doubleProbability = hitAverage * sluggish;

        if (random < singleProbability) {
            return BattingResult.HIT_SINGLE;
        } else if (random < singleProbability + doubleProbability) {
            return BattingResult.HIT_DOUBLE;
        }
        return BattingResult.OUT;

    }
}

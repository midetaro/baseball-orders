package org.example.domain.model.behavior;

import org.example.domain.code.BattingResult;

/**
 * 短距離バッター
 */
public class ShortDistanceAtBatBehavior implements AtBatBehavior {
    @Override
    public BattingResult batting(float hitAverage, float sluggish) {
        return BattingResult.HIT_SINGLE;
    }
}

package org.example.domain.behavior;

import org.example.domain.code.BattingResult;

/**
 * 短距離バッター
 */
public class ShortDistanceBattingBehavior implements BattingBehavior {
    @Override
    public BattingResult batting(float hitAverage, float sluggish) {
        return BattingResult.HIT_SINGLE;
    }
}

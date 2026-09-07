package com.example.baseballorders.simulator.domain.model.behavior;

import com.example.baseballorders.simulator.domain.code.BattingResult;
import org.springframework.stereotype.Component;

/** 長距離バッター */
@Component("longDistanceAtBat")
public class LongDistanceBattingBehavior implements AtBatBehavior {

    @Override
    public BattingResult batting(float hitAverage, float sluggish) {
        return BattingResult.HIT_HOMER;
    }
}

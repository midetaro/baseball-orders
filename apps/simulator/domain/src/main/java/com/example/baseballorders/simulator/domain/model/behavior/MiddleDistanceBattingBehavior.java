package com.example.baseballorders.simulator.domain.model.behavior;

import com.example.baseballorders.simulator.domain.code.BattingResult;
import org.springframework.stereotype.Component;

/** 中距離バッター */
@Component("middleDistanceAtBat")
public class MiddleDistanceBattingBehavior implements AtBatBehavior {

    @Override
    public BattingResult batting(float hitAverage, float sluggish) {
        return BattingResult.HIT_DOUBLE;
    }
}

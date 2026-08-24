package com.example.baseballorders.simulator.domain.model.behavior;

import com.example.baseballorders.simulator.domain.code.BattingResult;

public interface AtBatBehavior {

    BattingResult batting(float hitAverage, float sluggish);

}

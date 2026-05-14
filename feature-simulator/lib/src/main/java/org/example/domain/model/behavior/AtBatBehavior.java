package org.example.domain.model.behavior;

import org.example.domain.code.BattingResult;

public interface AtBatBehavior {

    BattingResult batting(float hitAverage, float sluggish);

}

package org.example.domain.behavior;

import org.example.domain.code.BattingResult;

public interface BattingBehavior {

    BattingResult batting(float hitAverage, float sluggish);

}

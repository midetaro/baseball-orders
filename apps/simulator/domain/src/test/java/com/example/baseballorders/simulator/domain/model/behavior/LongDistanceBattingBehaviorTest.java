package com.example.baseballorders.simulator.domain.model.behavior;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.baseballorders.simulator.domain.code.BattingResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LongDistanceBattingBehaviorTest {

    @Test
    @DisplayName("長距離バッターは本塁打を返す")
    void returnsHomeRun() {
        // given
        var behavior = new LongDistanceBattingBehavior();

        // when
        BattingResult result = behavior.batting(0.3f, 0.5f);

        // then
        assertAll(() -> assertEquals(BattingResult.HIT_HOMER, result));
    }
}

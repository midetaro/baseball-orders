package com.example.baseballorders.simulator.domain.game;

import static org.junit.jupiter.api.Assertions.*;

import com.example.baseballorders.simulator.domain.play.OutCount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BaseStateFactoryTest {
    @Test
    @DisplayName("ファクトリから作るStateは試合間で共有しない")
    void createsIndependentGameStates() {
        // given
        var first = GameStateTestFixture.context(null, null, null, OutCount.NO_OUT);
        var second = GameStateTestFixture.context(null, null, null, OutCount.NO_OUT);
        // when
        first.out();
        // then
        assertAll(
                () -> assertNotSame(first.getCurrentBaseState(), second.getCurrentBaseState()),
                () -> assertEquals(OutCount.ONE_OUT, first.getCurrentBaseState().getOutCount()),
                () -> assertEquals(OutCount.NO_OUT, second.getCurrentBaseState().getOutCount()));
    }
}

package com.example.baseballorders.simulator.domain.player.strategy.steal;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.baseballorders.simulator.domain.play.StealResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NowayStealStrategyTest {
    @Test
    @DisplayName("盗塁しない戦略は成功率にかかわらず両塁への試行を拒む")
    void neverAttemptsEitherBase() {
        // given
        var strategy = new NowayStealStrategy();
        // when
        var second = strategy.runToDouble(1.0f);
        var third = strategy.runToTriple(1.0f);
        // then
        assertAll(
                () -> assertEquals(StealResult.NOT_TRY, second),
                () -> assertEquals(StealResult.NOT_TRY, third));
    }
}

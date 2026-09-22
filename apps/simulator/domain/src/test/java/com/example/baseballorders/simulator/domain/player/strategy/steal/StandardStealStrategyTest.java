package com.example.baseballorders.simulator.domain.player.strategy.steal;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

import com.example.baseballorders.simulator.domain.play.StealResult;
import com.example.baseballorders.simulator.domain.player.strategy.RandomGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class StandardStealStrategyTest {
    @Test
    @DisplayName("StandardStealStrategyは成功確率内で二塁盗塁を成功させる")
    void succeedsAtSecond() {
        // given
        var strategy = new StandardStealStrategy();
        StealResult result;
        // when
        try (MockedStatic<RandomGenerator> random = mockStatic(RandomGenerator.class)) {
            random.when(RandomGenerator::nextFloat).thenReturn(0.9f);
            result = strategy.runToDouble(0.9f);
        }
        // then
        assertAll(() -> assertEquals(StealResult.SUCCESS, result));
    }
}

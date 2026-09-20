package com.example.baseballorders.simulator.domain.player.strategy.bunt;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.baseballorders.simulator.domain.play.BuntResult;
import com.example.baseballorders.simulator.domain.play.OutCount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class NowayBuntStrategyTest {

    @ParameterizedTest
    @EnumSource(OutCount.class)
    @DisplayName("バントしない戦略はアウトカウントにかかわらず試行しない")
    void neverAttemptsBunt(OutCount outCount) {
        // given
        var strategy = new NowayBuntStrategy();

        // when
        var result = strategy.bunt(1.0f, outCount);

        // then
        assertAll(() -> assertEquals(BuntResult.NOT_TRY, result));
    }
}

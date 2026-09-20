package com.example.baseballorders.simulator.domain.entity.behavior.bunt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.baseballorders.simulator.domain.code.BuntResult;
import com.example.baseballorders.simulator.domain.code.OutCount;
import com.example.baseballorders.simulator.domain.model.base.BasesState;
import com.example.baseballorders.simulator.domain.util.RandomGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class EagerBuntStrategyTest {
    @ParameterizedTest
    @EnumSource(OutCount.class)
    @DisplayName("積極戦略はStateのアウト数からバントの実行を判定する")
    void respectsOutCount(OutCount outs) {
        // given
        var state = mock(BasesState.class);
        when(state.getOutCount()).thenReturn(outs);
        var strategy = new EagerBuntStrategy();
        boolean attempts =
                switch (outs) {
                    case NO_OUT -> true;
                    case ONE_OUT -> true;
                    case TWO_OUT, THREE_OUT -> false;
                };
        try (var random = mockStatic(RandomGenerator.class)) {
            random.when(RandomGenerator::nextFloat).thenReturn(0.69f, 0.7f);
            // when
            var success = strategy.bunt(0.7f, state);
            var failure = strategy.bunt(0.7f, state);
            // then
            assertAll(
                    () -> assertEquals(attempts ? BuntResult.SUCCESS : BuntResult.NOT_TRY, success),
                    () -> assertEquals(attempts ? BuntResult.FAILURE : BuntResult.NOT_TRY, failure),
                    () -> random.verify(RandomGenerator::nextFloat, times(attempts ? 2 : 0)));
        }
    }
}

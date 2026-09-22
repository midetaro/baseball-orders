package com.example.baseballorders.simulator.domain.game;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.example.baseballorders.simulator.domain.play.OutCount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FirstDoubleBaseStateTest {
    @Test
    @DisplayName("FirstDoubleBaseStateの単打は走者と得点を更新する")
    void advancesRunnersOnSingle() {
        // given
        var batters = BatterTestDataFactory.mock();
        var first = batters.get(0);
        var second = batters.get(1);
        var third = batters.get(2);
        var batter = batters.get(3);
        var context = GameStateTestFixture.context(first, second, null, OutCount.NO_OUT);
        var state =
                assertInstanceOf(
                        FirstDoubleBaseState.class,
                        context.inningStateContext().currentBaseState());

        // when
        state.hitSingle(batter);

        // then
        var after = context.inningStateContext().currentBaseState();
        assertAll(
                () -> assertInstanceOf(FullBasesState.class, after),
                () -> assertSame(batter, after.runnerAt(Base.FIRST)),
                () -> assertSame(first, after.runnerAt(Base.SECOND)),
                () -> assertSame(second, after.runnerAt(Base.THIRD)),
                () -> assertEquals(0, context.getTotalScore()));
    }
}

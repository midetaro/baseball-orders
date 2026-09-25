package com.example.baseballorders.simulator.domain.game;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.example.baseballorders.simulator.domain.game.capability.Buntable;
import com.example.baseballorders.simulator.domain.game.capability.Stealable;
import com.example.baseballorders.simulator.domain.play.OutCount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 走者なし状態に固有の振る舞い。
 *
 * <p>全走者配置に共通する結果イベントの遷移は {@code BasesStateTransitionTest} が網羅する。ここでは
 * このクラスだけの差分、すなわち盗塁もバントも能力として持たないことを固定する。
 */
class NoBasesStateTest {

    @Test
    @DisplayName("NoBasesStateの単打は打者を一塁へ置き得点しない")
    void advancesRunnersOnSingle() {
        // given
        var batter = BatterTestDataFactory.mock().get(3);
        var context = GameStateTestFixture.context(null, null, null, OutCount.NO_OUT);
        var sut =
                assertInstanceOf(
                        NoBasesState.class, context.inningStateContext().currentBaseState());

        // when
        sut.hitSingle(batter);

        // then
        var after = context.inningStateContext().currentBaseState();
        assertAll(
                () -> assertInstanceOf(SingleBasesState.class, after),
                () -> assertSame(batter, after.runnerAt(Base.FIRST)),
                () -> assertNull(after.runnerAt(Base.SECOND)),
                () -> assertNull(after.runnerAt(Base.THIRD)),
                () -> assertEquals(0, context.getTotalScore()));
    }

    @Test
    @DisplayName("NoBasesStateは走者がいないため盗塁もバントも能力として持たない")
    void exposesNeitherStealNorBuntCapability() {
        // given
        var context = GameStateTestFixture.context(null, null, null, OutCount.NO_OUT);

        // when
        var sut = context.inningStateContext().currentBaseState();

        // then
        assertAll(
                () -> assertInstanceOf(NoBasesState.class, sut),
                () -> assertFalse(sut instanceof Stealable, "盗塁できないこと"),
                () -> assertFalse(sut instanceof Buntable, "バントできないこと"),
                () -> assertEquals(0, sut.runnerCount()));
    }
}

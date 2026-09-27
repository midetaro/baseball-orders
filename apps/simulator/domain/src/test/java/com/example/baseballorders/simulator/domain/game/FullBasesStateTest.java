package com.example.baseballorders.simulator.domain.game;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.example.baseballorders.simulator.domain.game.capability.SqueezeBuntable;
import com.example.baseballorders.simulator.domain.game.capability.Stealable;
import com.example.baseballorders.simulator.domain.play.OutCount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 満塁の状態に固有の振る舞い。
 *
 * <p>全走者配置に共通する結果イベントの遷移は {@code BasesStateTransitionTest} が網羅する。ここでは 走者が 3
 * 人いても進塁先がないため盗塁できないこと、そして {@link SqueezeBuntable} の default 実装、 特に {@code buntFailure} の両分岐を固定する。
 */
class FullBasesStateTest {

    @Test
    @DisplayName("FullBasesStateの単打は三塁走者を生還させ残る走者を一つ進める")
    void advancesRunnersOnSingle() {
        // given
        var batters = BatterTestDataFactory.mock();
        var first = batters.get(0);
        var second = batters.get(1);
        var third = batters.get(2);
        var batter = batters.get(3);
        var context = GameStateTestFixture.context(first, second, third, OutCount.NO_OUT);
        var sut =
                assertInstanceOf(
                        FullBasesState.class, context.inningStateContext().currentBaseState());

        // when
        sut.hitSingle(batter);

        // then
        var after = context.inningStateContext().currentBaseState();
        assertAll(
                () -> assertInstanceOf(FullBasesState.class, after),
                () -> assertSame(batter, after.runnerAt(Base.FIRST)),
                () -> assertSame(first, after.runnerAt(Base.SECOND)),
                () -> assertSame(second, after.runnerAt(Base.THIRD)),
                () -> assertEquals(1, context.getTotalScore()));
    }

    @Test
    @DisplayName("FullBasesStateは走者が3人いても進塁先がないため盗塁できない")
    void exposesSqueezeButNotSteal() {
        // given
        var batters = BatterTestDataFactory.mock();
        var context =
                GameStateTestFixture.context(
                        batters.get(0), batters.get(1), batters.get(2), OutCount.NO_OUT);

        // when
        var sut = context.inningStateContext().currentBaseState();

        // then
        assertAll(
                () -> assertInstanceOf(SqueezeBuntable.class, sut),
                () -> assertFalse(sut instanceof Stealable, "空いている塁がないため盗塁できないこと"),
                () -> assertEquals(3, sut.runnerCount()));
    }

    @Test
    @DisplayName("FullBasesStateのスクイズ成功は三塁走者を生還させ残る二人を塁に残す")
    void scoresThirdRunnerOnSqueezeSuccess() {
        // given
        var batters = BatterTestDataFactory.mock();
        var first = batters.get(0);
        var second = batters.get(1);
        var third = batters.get(2);
        var context = GameStateTestFixture.context(first, second, third, OutCount.NO_OUT);
        var sut =
                assertInstanceOf(
                        SqueezeBuntable.class, context.inningStateContext().currentBaseState());

        // when
        sut.buntSuccess();

        // then
        var after = context.inningStateContext().currentBaseState();
        assertAll(
                () -> assertInstanceOf(FirstDoubleBaseState.class, after),
                () -> assertSame(first, after.runnerAt(Base.FIRST)),
                () -> assertSame(second, after.runnerAt(Base.SECOND)),
                () -> assertNull(after.runnerAt(Base.THIRD)),
                () -> assertEquals(OutCount.ONE_OUT, after.getOutCount()),
                () -> assertEquals(1, context.getTotalScore()));
    }

    @Test
    @DisplayName("無死のスクイズ失敗は三塁走者と打者の二死を適用し残る二人を塁に残す")
    void appliesTwoOutsOnSqueezeFailureWithNoOut() {
        // given
        var batters = BatterTestDataFactory.mock();
        var first = batters.get(0);
        var second = batters.get(1);
        var third = batters.get(2);
        var context = GameStateTestFixture.context(first, second, third, OutCount.NO_OUT);
        var sut =
                assertInstanceOf(
                        SqueezeBuntable.class, context.inningStateContext().currentBaseState());

        // when
        sut.buntFailure();

        // then
        var after = context.inningStateContext().currentBaseState();
        assertAll(
                () -> assertInstanceOf(FirstDoubleBaseState.class, after),
                () -> assertSame(first, after.runnerAt(Base.FIRST)),
                () -> assertSame(second, after.runnerAt(Base.SECOND)),
                () -> assertNull(after.runnerAt(Base.THIRD)),
                () -> assertEquals(OutCount.TWO_OUT, after.getOutCount()),
                () -> assertEquals(1, context.getInning()),
                () -> assertEquals(0, context.getTotalScore()));
    }

    @Test
    @DisplayName("二死のスクイズ失敗は三死でイニングが終わるため二死目を加えない")
    void completesInningOnSqueezeFailureWithTwoOuts() {
        // given
        var batters = BatterTestDataFactory.mock();
        var context =
                GameStateTestFixture.context(
                        batters.get(0), batters.get(1), batters.get(2), OutCount.TWO_OUT);
        var sut =
                assertInstanceOf(
                        SqueezeBuntable.class, context.inningStateContext().currentBaseState());

        // when
        sut.buntFailure();

        // then
        var after = context.inningStateContext().currentBaseState();
        assertAll(
                () -> assertInstanceOf(NoBasesState.class, after),
                () -> assertEquals(0, after.runnerCount()),
                () -> assertEquals(OutCount.NO_OUT, after.getOutCount()),
                () -> assertEquals(2, context.getInning()),
                () -> assertEquals(0, context.getTotalScore()));
    }
}

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
 * 三塁走者だけの状態に固有の振る舞い。
 *
 * <p>全走者配置に共通する結果イベントの遷移は {@code BasesStateTransitionTest} が網羅する。ここでは {@link SqueezeBuntable} の
 * default 実装、特に {@code buntFailure} が持つ「二死目を加えるかどうか」の 両分岐を固定する。
 */
class ThirdBaseStateTest {

    @Test
    @DisplayName("ThirdBaseStateの単打は三塁走者を生還させ打者を一塁へ置く")
    void advancesRunnersOnSingle() {
        // given
        var batters = BatterTestDataFactory.mock();
        var third = batters.get(2);
        var batter = batters.get(3);
        var context = GameStateTestFixture.context(null, null, third, OutCount.NO_OUT);
        var sut =
                assertInstanceOf(
                        ThirdBaseState.class, context.inningStateContext().currentBaseState());

        // when
        sut.hitSingle(batter);

        // then
        var after = context.inningStateContext().currentBaseState();
        assertAll(
                () -> assertInstanceOf(SingleBasesState.class, after),
                () -> assertSame(batter, after.runnerAt(Base.FIRST)),
                () -> assertNull(after.runnerAt(Base.SECOND)),
                () -> assertNull(after.runnerAt(Base.THIRD)),
                () -> assertEquals(1, context.getTotalScore()));
    }

    @Test
    @DisplayName("ThirdBaseStateは一塁と二塁が空なのでスクイズだけを能力として持つ")
    void exposesSqueezeButNotSteal() {
        // given
        var third = BatterTestDataFactory.mock().get(2);
        var context = GameStateTestFixture.context(null, null, third, OutCount.NO_OUT);

        // when
        var sut = context.inningStateContext().currentBaseState();

        // then
        assertAll(
                () -> assertInstanceOf(SqueezeBuntable.class, sut),
                () -> assertFalse(sut instanceof Stealable, "盗塁できないこと"));
    }

    @Test
    @DisplayName("ThirdBaseStateのスクイズ成功は三塁走者を生還させ一死を加える")
    void scoresRunnerOnSqueezeSuccess() {
        // given
        var third = BatterTestDataFactory.mock().get(2);
        var context = GameStateTestFixture.context(null, null, third, OutCount.NO_OUT);
        var sut =
                assertInstanceOf(
                        SqueezeBuntable.class, context.inningStateContext().currentBaseState());

        // when
        sut.buntSuccess();

        // then
        var after = context.inningStateContext().currentBaseState();
        assertAll(
                () -> assertInstanceOf(NoBasesState.class, after),
                () -> assertEquals(0, after.runnerCount()),
                () -> assertEquals(OutCount.ONE_OUT, after.getOutCount()),
                () -> assertEquals(1, context.getTotalScore()));
    }

    @Test
    @DisplayName("無死のスクイズ失敗は三塁走者と打者の二死を適用し得点しない")
    void appliesTwoOutsOnSqueezeFailureWithNoOut() {
        // given
        var third = BatterTestDataFactory.mock().get(2);
        var context = GameStateTestFixture.context(null, null, third, OutCount.NO_OUT);
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
                () -> assertEquals(OutCount.TWO_OUT, after.getOutCount()),
                () -> assertEquals(1, context.getInning()),
                () -> assertEquals(0, context.getTotalScore()));
    }

    @Test
    @DisplayName("二死のスクイズ失敗は三死でイニングが終わるため二死目を加えない")
    void completesInningOnSqueezeFailureWithTwoOuts() {
        // given
        var third = BatterTestDataFactory.mock().get(2);
        var context = GameStateTestFixture.context(null, null, third, OutCount.TWO_OUT);
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

package com.example.baseballorders.simulator.domain.game;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.example.baseballorders.simulator.domain.game.capability.SqueezeBuntable;
import com.example.baseballorders.simulator.domain.game.capability.StealableToDoubleBase;
import com.example.baseballorders.simulator.domain.play.OutCount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 一三塁の状態に固有の振る舞い。
 *
 * <p>全走者配置に共通する結果イベントの遷移は {@code BasesStateTransitionTest} が網羅する。ここでは 盗塁は {@link
 * StealableToDoubleBase}、バントは {@link SqueezeBuntable} という このクラス固有の能力の組み合わせと、{@code buntFailure}
 * の両分岐を固定する。
 */
class FirstThirdBaseStateTest {

    @Test
    @DisplayName("FirstThirdBaseStateの単打は三塁走者を生還させ一塁走者を二塁へ進める")
    void advancesRunnersOnSingle() {
        // given
        var batters = BatterTestDataFactory.mock();
        var first = batters.get(0);
        var third = batters.get(2);
        var batter = batters.get(3);
        var context = GameStateTestFixture.context(first, null, third, OutCount.NO_OUT);
        var sut =
                assertInstanceOf(
                        FirstThirdBaseState.class, context.inningStateContext().currentBaseState());

        // when
        sut.hitSingle(batter);

        // then
        var after = context.inningStateContext().currentBaseState();
        assertAll(
                () -> assertInstanceOf(FirstDoubleBaseState.class, after),
                () -> assertSame(batter, after.runnerAt(Base.FIRST)),
                () -> assertSame(first, after.runnerAt(Base.SECOND)),
                () -> assertNull(after.runnerAt(Base.THIRD)),
                () -> assertEquals(1, context.getTotalScore()));
    }

    @Test
    @DisplayName("FirstThirdBaseStateは一塁走者に二塁への盗塁を担当させる")
    void exposesStealFromFirstToSecond() {
        // given
        var batters = BatterTestDataFactory.mock();
        var first = batters.get(0);
        var third = batters.get(2);
        var context = GameStateTestFixture.context(first, null, third, OutCount.NO_OUT);

        // when
        var sut =
                assertInstanceOf(
                        StealableToDoubleBase.class,
                        context.inningStateContext().currentBaseState());

        // then
        assertAll(
                () -> assertEquals(Base.FIRST, sut.sourceBase()),
                () -> assertEquals(Base.SECOND, sut.targetBase()),
                () -> assertSame(first, sut.runnerOnFirst()),
                () -> assertSame(first, sut.runner()));
    }

    @Test
    @DisplayName("FirstThirdBaseStateの盗塁成功は一塁走者を二塁へ移し三塁走者を残す")
    void movesFirstRunnerOnStealSuccess() {
        // given
        var batters = BatterTestDataFactory.mock();
        var first = batters.get(0);
        var third = batters.get(2);
        var context = GameStateTestFixture.context(first, null, third, OutCount.NO_OUT);
        var sut =
                assertInstanceOf(
                        StealableToDoubleBase.class,
                        context.inningStateContext().currentBaseState());

        // when
        sut.stealSuccess();

        // then
        var after = context.inningStateContext().currentBaseState();
        assertAll(
                () -> assertInstanceOf(DoubleThirdBaseState.class, after),
                () -> assertNull(after.runnerAt(Base.FIRST)),
                () -> assertSame(first, after.runnerAt(Base.SECOND)),
                () -> assertSame(third, after.runnerAt(Base.THIRD)),
                () -> assertEquals(OutCount.NO_OUT, after.getOutCount()),
                () -> assertEquals(0, context.getTotalScore()));
    }

    @Test
    @DisplayName("FirstThirdBaseStateの盗塁死は一塁走者だけを除いて一死を加える")
    void retiresFirstRunnerOnStealFailure() {
        // given
        var batters = BatterTestDataFactory.mock();
        var first = batters.get(0);
        var third = batters.get(2);
        var context = GameStateTestFixture.context(first, null, third, OutCount.NO_OUT);
        var sut =
                assertInstanceOf(
                        StealableToDoubleBase.class,
                        context.inningStateContext().currentBaseState());

        // when
        sut.stealFailure();

        // then
        var after = context.inningStateContext().currentBaseState();
        assertAll(
                () -> assertInstanceOf(ThirdBaseState.class, after),
                () -> assertNull(after.runnerAt(Base.FIRST)),
                () -> assertSame(third, after.runnerAt(Base.THIRD)),
                () -> assertEquals(OutCount.ONE_OUT, after.getOutCount()),
                () -> assertEquals(0, context.getTotalScore()));
    }

    @Test
    @DisplayName("FirstThirdBaseStateのスクイズ成功は三塁走者を生還させ一塁走者を残す")
    void scoresThirdRunnerOnSqueezeSuccess() {
        // given
        var batters = BatterTestDataFactory.mock();
        var first = batters.get(0);
        var third = batters.get(2);
        var context = GameStateTestFixture.context(first, null, third, OutCount.NO_OUT);
        var sut =
                assertInstanceOf(
                        SqueezeBuntable.class, context.inningStateContext().currentBaseState());

        // when
        sut.buntSuccess();

        // then
        var after = context.inningStateContext().currentBaseState();
        assertAll(
                () -> assertInstanceOf(SingleBasesState.class, after),
                () -> assertSame(first, after.runnerAt(Base.FIRST)),
                () -> assertNull(after.runnerAt(Base.THIRD)),
                () -> assertEquals(OutCount.ONE_OUT, after.getOutCount()),
                () -> assertEquals(1, context.getTotalScore()));
    }

    @Test
    @DisplayName("無死のスクイズ失敗は三塁走者と打者の二死を適用し一塁走者を残す")
    void appliesTwoOutsOnSqueezeFailureWithNoOut() {
        // given
        var batters = BatterTestDataFactory.mock();
        var first = batters.get(0);
        var third = batters.get(2);
        var context = GameStateTestFixture.context(first, null, third, OutCount.NO_OUT);
        var sut =
                assertInstanceOf(
                        SqueezeBuntable.class, context.inningStateContext().currentBaseState());

        // when
        sut.buntFailure();

        // then
        var after = context.inningStateContext().currentBaseState();
        assertAll(
                () -> assertInstanceOf(SingleBasesState.class, after),
                () -> assertSame(first, after.runnerAt(Base.FIRST)),
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
        var first = batters.get(0);
        var third = batters.get(2);
        var context = GameStateTestFixture.context(first, null, third, OutCount.TWO_OUT);
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

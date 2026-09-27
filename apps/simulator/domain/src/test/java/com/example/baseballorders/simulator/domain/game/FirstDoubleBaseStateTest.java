package com.example.baseballorders.simulator.domain.game;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.example.baseballorders.simulator.domain.game.capability.AdvancingBuntable;
import com.example.baseballorders.simulator.domain.game.capability.StealableToTripleBase;
import com.example.baseballorders.simulator.domain.play.OutCount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 一二塁の状態に固有の振る舞い。
 *
 * <p>全走者配置に共通する結果イベントの遷移は {@code BasesStateTransitionTest} が網羅する。ここでは 走者が 2 人いても {@link
 * StealableToTripleBase} として二塁走者だけが三塁を狙うという、このクラス固有の選択と {@link AdvancingBuntable} の default
 * 実装を固定する。
 */
class FirstDoubleBaseStateTest {

    @Test
    @DisplayName("FirstDoubleBaseStateの単打は各走者を一つ進めて満塁にする")
    void advancesRunnersOnSingle() {
        // given
        var batters = BatterTestDataFactory.mock();
        var first = batters.get(0);
        var second = batters.get(1);
        var batter = batters.get(3);
        var context = GameStateTestFixture.context(first, second, null, OutCount.NO_OUT);
        var sut =
                assertInstanceOf(
                        FirstDoubleBaseState.class,
                        context.inningStateContext().currentBaseState());

        // when
        sut.hitSingle(batter);

        // then
        var after = context.inningStateContext().currentBaseState();
        assertAll(
                () -> assertInstanceOf(FullBasesState.class, after),
                () -> assertSame(batter, after.runnerAt(Base.FIRST)),
                () -> assertSame(first, after.runnerAt(Base.SECOND)),
                () -> assertSame(second, after.runnerAt(Base.THIRD)),
                () -> assertEquals(0, context.getTotalScore()));
    }

    @Test
    @DisplayName("FirstDoubleBaseStateは一塁走者ではなく二塁走者に三塁への盗塁を担当させる")
    void exposesStealFromSecondRunnerOnly() {
        // given
        var batters = BatterTestDataFactory.mock();
        var first = batters.get(0);
        var second = batters.get(1);
        var context = GameStateTestFixture.context(first, second, null, OutCount.NO_OUT);

        // when
        var sut =
                assertInstanceOf(
                        StealableToTripleBase.class,
                        context.inningStateContext().currentBaseState());

        // then
        assertAll(
                () -> assertEquals(Base.SECOND, sut.sourceBase()),
                () -> assertEquals(Base.THIRD, sut.targetBase()),
                () -> assertSame(second, sut.runnerOnSecond()),
                () -> assertSame(second, sut.runner()));
    }

    @Test
    @DisplayName("FirstDoubleBaseStateの盗塁成功は二塁走者だけを三塁へ移し一塁走者を残す")
    void movesOnlySecondRunnerOnStealSuccess() {
        // given
        var batters = BatterTestDataFactory.mock();
        var first = batters.get(0);
        var second = batters.get(1);
        var context = GameStateTestFixture.context(first, second, null, OutCount.NO_OUT);
        var sut =
                assertInstanceOf(
                        StealableToTripleBase.class,
                        context.inningStateContext().currentBaseState());

        // when
        sut.stealSuccess();

        // then
        var after = context.inningStateContext().currentBaseState();
        assertAll(
                () -> assertInstanceOf(FirstThirdBaseState.class, after),
                () -> assertSame(first, after.runnerAt(Base.FIRST)),
                () -> assertNull(after.runnerAt(Base.SECOND)),
                () -> assertSame(second, after.runnerAt(Base.THIRD)),
                () -> assertEquals(OutCount.NO_OUT, after.getOutCount()),
                () -> assertEquals(0, context.getTotalScore()));
    }

    @Test
    @DisplayName("FirstDoubleBaseStateの盗塁死は二塁走者だけを除いて一死を加える")
    void retiresOnlySecondRunnerOnStealFailure() {
        // given
        var batters = BatterTestDataFactory.mock();
        var first = batters.get(0);
        var second = batters.get(1);
        var context = GameStateTestFixture.context(first, second, null, OutCount.NO_OUT);
        var sut =
                assertInstanceOf(
                        StealableToTripleBase.class,
                        context.inningStateContext().currentBaseState());

        // when
        sut.stealFailure();

        // then
        var after = context.inningStateContext().currentBaseState();
        assertAll(
                () -> assertInstanceOf(SingleBasesState.class, after),
                () -> assertSame(first, after.runnerAt(Base.FIRST)),
                () -> assertNull(after.runnerAt(Base.SECOND)),
                () -> assertEquals(OutCount.ONE_OUT, after.getOutCount()),
                () -> assertEquals(0, context.getTotalScore()));
    }

    @Test
    @DisplayName("FirstDoubleBaseStateの進塁バント成功は両走者を一つ進めて一死を加える")
    void advancesBothRunnersOnAdvancingBuntSuccess() {
        // given
        var batters = BatterTestDataFactory.mock();
        var first = batters.get(0);
        var second = batters.get(1);
        var context = GameStateTestFixture.context(first, second, null, OutCount.NO_OUT);
        var sut =
                assertInstanceOf(
                        AdvancingBuntable.class, context.inningStateContext().currentBaseState());

        // when
        sut.buntSuccess();

        // then
        var after = context.inningStateContext().currentBaseState();
        assertAll(
                () -> assertInstanceOf(DoubleThirdBaseState.class, after),
                () -> assertNull(after.runnerAt(Base.FIRST)),
                () -> assertSame(first, after.runnerAt(Base.SECOND)),
                () -> assertSame(second, after.runnerAt(Base.THIRD)),
                () -> assertEquals(OutCount.ONE_OUT, after.getOutCount()),
                () -> assertEquals(0, context.getTotalScore()));
    }

    @Test
    @DisplayName("FirstDoubleBaseStateの進塁バント失敗は走者を進めず一死だけを加える")
    void keepsRunnersOnAdvancingBuntFailure() {
        // given
        var batters = BatterTestDataFactory.mock();
        var first = batters.get(0);
        var second = batters.get(1);
        var context = GameStateTestFixture.context(first, second, null, OutCount.NO_OUT);
        var sut =
                assertInstanceOf(
                        AdvancingBuntable.class, context.inningStateContext().currentBaseState());

        // when
        sut.buntFailure();

        // then
        var after = context.inningStateContext().currentBaseState();
        assertAll(
                () -> assertInstanceOf(FirstDoubleBaseState.class, after),
                () -> assertSame(first, after.runnerAt(Base.FIRST)),
                () -> assertSame(second, after.runnerAt(Base.SECOND)),
                () -> assertEquals(OutCount.ONE_OUT, after.getOutCount()),
                () -> assertEquals(0, context.getTotalScore()));
    }
}

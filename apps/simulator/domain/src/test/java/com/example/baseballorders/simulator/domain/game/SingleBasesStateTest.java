package com.example.baseballorders.simulator.domain.game;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.example.baseballorders.simulator.domain.game.capability.AdvancingBuntable;
import com.example.baseballorders.simulator.domain.game.capability.StealableToDoubleBase;
import com.example.baseballorders.simulator.domain.play.OutCount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 一塁走者だけの状態に固有の振る舞い。
 *
 * <p>全走者配置に共通する結果イベントの遷移は {@code BasesStateTransitionTest} が網羅する。ここでは このクラスが選ぶ能力、すなわち {@link
 * StealableToDoubleBase} と {@link AdvancingBuntable} の default 実装を固定する。
 */
class SingleBasesStateTest {

    @Test
    @DisplayName("SingleBasesStateの単打は打者を一塁へ置き走者を二塁へ進める")
    void advancesRunnersOnSingle() {
        // given
        var batters = BatterTestDataFactory.mock();
        var first = batters.get(0);
        var batter = batters.get(3);
        var context = GameStateTestFixture.context(first, null, null, OutCount.NO_OUT);
        var sut =
                assertInstanceOf(
                        SingleBasesState.class, context.inningStateContext().currentBaseState());

        // when
        sut.hitSingle(batter);

        // then
        var after = context.inningStateContext().currentBaseState();
        assertAll(
                () -> assertInstanceOf(FirstDoubleBaseState.class, after),
                () -> assertSame(batter, after.runnerAt(Base.FIRST)),
                () -> assertSame(first, after.runnerAt(Base.SECOND)),
                () -> assertNull(after.runnerAt(Base.THIRD)),
                () -> assertEquals(0, context.getTotalScore()));
    }

    @Test
    @DisplayName("SingleBasesStateは一塁走者に二塁への盗塁を担当させる")
    void exposesStealFromFirstToSecond() {
        // given
        var first = BatterTestDataFactory.mock().get(0);
        var context = GameStateTestFixture.context(first, null, null, OutCount.NO_OUT);

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
    @DisplayName("SingleBasesStateの盗塁成功は一塁走者を二塁へ移しアウトを増やさない")
    void movesRunnerToSecondOnStealSuccess() {
        // given
        var first = BatterTestDataFactory.mock().get(0);
        var context = GameStateTestFixture.context(first, null, null, OutCount.NO_OUT);
        var sut =
                assertInstanceOf(
                        StealableToDoubleBase.class,
                        context.inningStateContext().currentBaseState());

        // when
        sut.stealSuccess();

        // then
        var after = context.inningStateContext().currentBaseState();
        assertAll(
                () -> assertInstanceOf(DoubleBaseState.class, after),
                () -> assertNull(after.runnerAt(Base.FIRST)),
                () -> assertSame(first, after.runnerAt(Base.SECOND)),
                () -> assertEquals(OutCount.NO_OUT, after.getOutCount()),
                () -> assertEquals(0, context.getTotalScore()));
    }

    @Test
    @DisplayName("SingleBasesStateの盗塁死は一塁走者を除いて一死を加える")
    void retiresRunnerOnStealFailure() {
        // given
        var first = BatterTestDataFactory.mock().get(0);
        var context = GameStateTestFixture.context(first, null, null, OutCount.NO_OUT);
        var sut =
                assertInstanceOf(
                        StealableToDoubleBase.class,
                        context.inningStateContext().currentBaseState());

        // when
        sut.stealFailure();

        // then
        var after = context.inningStateContext().currentBaseState();
        assertAll(
                () -> assertInstanceOf(NoBasesState.class, after),
                () -> assertEquals(0, after.runnerCount()),
                () -> assertEquals(OutCount.ONE_OUT, after.getOutCount()),
                () -> assertEquals(0, context.getTotalScore()));
    }

    @Test
    @DisplayName("SingleBasesStateの進塁バント成功は一塁走者を二塁へ進めて一死を加える")
    void advancesRunnerOnAdvancingBuntSuccess() {
        // given
        var first = BatterTestDataFactory.mock().get(0);
        var context = GameStateTestFixture.context(first, null, null, OutCount.NO_OUT);
        var sut =
                assertInstanceOf(
                        AdvancingBuntable.class, context.inningStateContext().currentBaseState());

        // when
        sut.buntSuccess();

        // then
        var after = context.inningStateContext().currentBaseState();
        assertAll(
                () -> assertInstanceOf(DoubleBaseState.class, after),
                () -> assertNull(after.runnerAt(Base.FIRST)),
                () -> assertSame(first, after.runnerAt(Base.SECOND)),
                () -> assertEquals(OutCount.ONE_OUT, after.getOutCount()),
                () -> assertEquals(0, context.getTotalScore()));
    }

    @Test
    @DisplayName("SingleBasesStateの進塁バント失敗は走者を進めず一死だけを加える")
    void keepsRunnerOnAdvancingBuntFailure() {
        // given
        var first = BatterTestDataFactory.mock().get(0);
        var context = GameStateTestFixture.context(first, null, null, OutCount.NO_OUT);
        var sut =
                assertInstanceOf(
                        AdvancingBuntable.class, context.inningStateContext().currentBaseState());

        // when
        sut.buntFailure();

        // then
        var after = context.inningStateContext().currentBaseState();
        assertAll(
                () -> assertInstanceOf(SingleBasesState.class, after),
                () -> assertSame(first, after.runnerAt(Base.FIRST)),
                () -> assertEquals(OutCount.ONE_OUT, after.getOutCount()),
                () -> assertEquals(0, context.getTotalScore()));
    }
}

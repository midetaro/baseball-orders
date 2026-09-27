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
 * 二塁走者だけの状態に固有の振る舞い。
 *
 * <p>全走者配置に共通する結果イベントの遷移は {@code BasesStateTransitionTest} が網羅する。ここでは このクラスが選ぶ能力、すなわち {@link
 * StealableToTripleBase} と {@link AdvancingBuntable} の default 実装を固定する。
 */
class DoubleBaseStateTest {

    @Test
    @DisplayName("DoubleBaseStateの単打は打者を一塁へ置き二塁走者を三塁へ進める")
    void advancesRunnersOnSingle() {
        // given
        var batters = BatterTestDataFactory.mock();
        var second = batters.get(1);
        var batter = batters.get(3);
        var context = GameStateTestFixture.context(null, second, null, OutCount.NO_OUT);
        var sut =
                assertInstanceOf(
                        DoubleBaseState.class, context.inningStateContext().currentBaseState());

        // when
        sut.hitSingle(batter);

        // then
        var after = context.inningStateContext().currentBaseState();
        assertAll(
                () -> assertInstanceOf(FirstThirdBaseState.class, after),
                () -> assertSame(batter, after.runnerAt(Base.FIRST)),
                () -> assertNull(after.runnerAt(Base.SECOND)),
                () -> assertSame(second, after.runnerAt(Base.THIRD)),
                () -> assertEquals(0, context.getTotalScore()));
    }

    @Test
    @DisplayName("DoubleBaseStateは二塁走者に三塁への盗塁を担当させる")
    void exposesStealFromSecondToThird() {
        // given
        var second = BatterTestDataFactory.mock().get(1);
        var context = GameStateTestFixture.context(null, second, null, OutCount.NO_OUT);

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
    @DisplayName("DoubleBaseStateの盗塁成功は二塁走者を三塁へ移しアウトを増やさない")
    void movesRunnerToThirdOnStealSuccess() {
        // given
        var second = BatterTestDataFactory.mock().get(1);
        var context = GameStateTestFixture.context(null, second, null, OutCount.NO_OUT);
        var sut =
                assertInstanceOf(
                        StealableToTripleBase.class,
                        context.inningStateContext().currentBaseState());

        // when
        sut.stealSuccess();

        // then
        var after = context.inningStateContext().currentBaseState();
        assertAll(
                () -> assertInstanceOf(ThirdBaseState.class, after),
                () -> assertNull(after.runnerAt(Base.SECOND)),
                () -> assertSame(second, after.runnerAt(Base.THIRD)),
                () -> assertEquals(OutCount.NO_OUT, after.getOutCount()),
                () -> assertEquals(0, context.getTotalScore()));
    }

    @Test
    @DisplayName("DoubleBaseStateの盗塁死は二塁走者を除いて一死を加える")
    void retiresRunnerOnStealFailure() {
        // given
        var second = BatterTestDataFactory.mock().get(1);
        var context = GameStateTestFixture.context(null, second, null, OutCount.NO_OUT);
        var sut =
                assertInstanceOf(
                        StealableToTripleBase.class,
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
    @DisplayName("DoubleBaseStateの進塁バント成功は二塁走者を三塁へ進めて一死を加える")
    void advancesRunnerOnAdvancingBuntSuccess() {
        // given
        var second = BatterTestDataFactory.mock().get(1);
        var context = GameStateTestFixture.context(null, second, null, OutCount.NO_OUT);
        var sut =
                assertInstanceOf(
                        AdvancingBuntable.class, context.inningStateContext().currentBaseState());

        // when
        sut.buntSuccess();

        // then
        var after = context.inningStateContext().currentBaseState();
        assertAll(
                () -> assertInstanceOf(ThirdBaseState.class, after),
                () -> assertNull(after.runnerAt(Base.SECOND)),
                () -> assertSame(second, after.runnerAt(Base.THIRD)),
                () -> assertEquals(OutCount.ONE_OUT, after.getOutCount()),
                () -> assertEquals(0, context.getTotalScore()));
    }

    @Test
    @DisplayName("DoubleBaseStateの進塁バント失敗は走者を進めず一死だけを加える")
    void keepsRunnerOnAdvancingBuntFailure() {
        // given
        var second = BatterTestDataFactory.mock().get(1);
        var context = GameStateTestFixture.context(null, second, null, OutCount.NO_OUT);
        var sut =
                assertInstanceOf(
                        AdvancingBuntable.class, context.inningStateContext().currentBaseState());

        // when
        sut.buntFailure();

        // then
        var after = context.inningStateContext().currentBaseState();
        assertAll(
                () -> assertInstanceOf(DoubleBaseState.class, after),
                () -> assertSame(second, after.runnerAt(Base.SECOND)),
                () -> assertEquals(OutCount.ONE_OUT, after.getOutCount()),
                () -> assertEquals(0, context.getTotalScore()));
    }
}

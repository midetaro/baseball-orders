package com.example.baseballorders.simulator.domain.game;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.baseballorders.simulator.domain.play.BattingResult;
import com.example.baseballorders.simulator.domain.play.BuntResult;
import com.example.baseballorders.simulator.domain.play.OutCount;
import com.example.baseballorders.simulator.domain.play.StealResult;
import com.example.baseballorders.simulator.domain.player.BatterEntity;
import com.example.baseballorders.simulator.domain.player.LineUpEntity;
import com.example.baseballorders.simulator.domain.statistics.GameCompletionObserver;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class GameStateLifecycleTest {
    private static BatterEntity batter() {
        var batter = mock(BatterEntity.class);
        when(batter.observedBy(any())).thenReturn(batter);
        return batter;
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 9})
    @DisplayName("二死から盗塁死になったら打撃せず三死を処理し未打撃の打者を引き継ぐ")
    void thirdOutStealDoesNotConsumeBatter(int inning) {
        // given
        var first = batter();
        var second = batter();
        when(first.swing(0)).thenReturn(BattingResult.HIT_SINGLE);
        when(first.stealToDouble()).thenReturn(StealResult.FAILURE);
        when(second.swing(0)).thenReturn(BattingResult.STRIKEOUT);
        var context = new GameBattingContext(new LineUpEntity(List.of(first, second)));
        for (int i = 0; i < (inning - 1) * 3 + 2; i++) {
            context.out();
        }
        context.nextAtBat();

        // when
        context.nextAtBat();
        boolean gameOverAfterSteal = context.isGameOver();
        OutCount outsAfterSteal = context.getCurrentBaseState().getOutCount();
        int runnersAfterSteal = context.getCurrentBaseState().runnerCount();
        context.nextAtBat();

        // then
        assertAll(
                () -> assertEquals(inning == 9, gameOverAfterSteal),
                () -> assertEquals(OutCount.NO_OUT, outsAfterSteal),
                () -> assertEquals(0, runnersAfterSteal),
                () -> verify(first, times(1)).swing(0),
                () -> verify(second, times(inning == 9 ? 0 : 1)).swing(0),
                () -> verify(second, never()).bunt(any(), any()),
                () ->
                        assertEquals(
                                inning == 9 ? OutCount.NO_OUT : OutCount.ONE_OUT,
                                context.getCurrentBaseState().getOutCount()));
    }

    @Test
    @DisplayName("九回終了で初期化し通知を一度だけ行い以後の全イベントを無視する")
    void ignoresEventsAfterCompletion() {
        // given
        var observer = mock(GameCompletionObserver.class);
        var batter = batter();
        var context =
                new GameBattingContext(new LineUpEntity(Collections.nCopies(9, batter)), observer);
        context.hitHomer();
        context.hitSingle(batter);
        // when
        for (int i = 0; i < 27; i++) {
            context.out();
        }
        var finalState = context.getCurrentBaseState();
        context.out();
        context.hitSingle(batter);
        context.hitDouble(batter);
        context.hitTriple(batter);
        context.hitHomer();
        context.walk(batter);
        context.buntNotTry();
        context.buntFailure();
        context.buntSuccess();
        context.stealNotTry();
        context.stealFailure();
        context.stealSuccess();
        context.nextAtBat();
        context.completeInning();
        // then
        assertAll(
                () -> assertTrue(context.isGameOver()),
                () -> assertSame(finalState, context.getCurrentBaseState()),
                () -> assertEquals(OutCount.NO_OUT, finalState.getOutCount()),
                () -> assertEquals(0, finalState.runnerCount()),
                () -> assertEquals(1, context.getTotalScore()),
                () -> verify(observer, times(1)).onGameCompleted(eq(1L), any()),
                () -> verify(batter, never()).swing(anyInt()));
    }

    @ParameterizedTest
    @EnumSource(BattingResult.class)
    @DisplayName("打撃結果の各定数を対応するContextイベントへ送る")
    void dispatchesBattingResults(BattingResult result) {
        // given
        var batter = batter();
        when(batter.swing(0)).thenReturn(result);
        var context = new GameBattingContext(new LineUpEntity(List.of(batter)));
        // when
        context.nextAtBat();
        // then
        assertAll(
                () ->
                        assertEquals(
                                result == BattingResult.STRIKEOUT
                                                || result == BattingResult.BATTED_OUT
                                        ? OutCount.ONE_OUT
                                        : OutCount.NO_OUT,
                                context.getCurrentBaseState().getOutCount()),
                () ->
                        assertEquals(
                                result == BattingResult.HIT_HOMER ? 1 : 0, context.getTotalScore()),
                () ->
                        assertEquals(
                                result == BattingResult.HIT_SINGLE || result == BattingResult.WALK,
                                context.getCurrentBaseState().isOccupied(Base.FIRST)),
                () ->
                        assertEquals(
                                result == BattingResult.HIT_DOUBLE,
                                context.getCurrentBaseState().isOccupied(Base.SECOND)),
                () ->
                        assertEquals(
                                result == BattingResult.HIT_TRIPLE,
                                context.getCurrentBaseState().isOccupied(Base.THIRD)));
    }

    @ParameterizedTest
    @EnumSource(StealResult.class)
    @DisplayName("盗塁結果を適用してから打撃時の走者数を判定する")
    void dispatchesStealResults(StealResult result) {
        // given
        var runner = batter();
        var hitter = batter();
        when(runner.stealToTriple()).thenReturn(result);
        when(hitter.bunt(any(), any())).thenReturn(BuntResult.NOT_TRY);
        when(hitter.swing(anyInt())).thenReturn(BattingResult.HIT_HOMER);
        var context = new GameBattingContext(new LineUpEntity(List.of(hitter)));
        context.hitDouble(runner);
        // when
        context.nextAtBat();
        // then
        assertAll(
                () -> verify(hitter).swing(result == StealResult.FAILURE ? 0 : 1),
                () -> assertEquals(result == StealResult.FAILURE ? 1 : 2, context.getTotalScore()),
                () ->
                        assertEquals(
                                result == StealResult.FAILURE ? OutCount.ONE_OUT : OutCount.NO_OUT,
                                context.getCurrentBaseState().getOutCount()));
    }

    @ParameterizedTest
    @EnumSource(
            value = BuntResult.class,
            names = {"SUCCESS", "FAILURE"})
    @DisplayName("バントの成功失敗を適用した打席ではヒッティングしない")
    void dispatchesBuntResults(BuntResult result) {
        // given
        var runner = batter();
        var hitter = batter();
        when(runner.stealToDouble()).thenReturn(StealResult.NOT_TRY);
        when(hitter.bunt(any(), any())).thenReturn(result);
        var context = new GameBattingContext(new LineUpEntity(List.of(hitter)));
        context.hitSingle(runner);
        // when
        context.nextAtBat();
        // then
        assertAll(
                () -> verify(hitter, never()).swing(anyInt()),
                () -> assertEquals(OutCount.ONE_OUT, context.getCurrentBaseState().getOutCount()),
                () ->
                        assertEquals(
                                result == BuntResult.SUCCESS,
                                context.getCurrentBaseState().isOccupied(Base.SECOND)),
                () ->
                        assertEquals(
                                result == BuntResult.FAILURE,
                                context.getCurrentBaseState().isOccupied(Base.FIRST)));
    }
}

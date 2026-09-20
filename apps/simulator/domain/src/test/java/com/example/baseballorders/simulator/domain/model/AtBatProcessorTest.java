package com.example.baseballorders.simulator.domain.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.baseballorders.simulator.domain.code.BattingResult;
import com.example.baseballorders.simulator.domain.code.BuntResult;
import com.example.baseballorders.simulator.domain.code.OutCount;
import com.example.baseballorders.simulator.domain.code.StealResult;
import com.example.baseballorders.simulator.domain.entity.player.BatterEntity;
import com.example.baseballorders.simulator.domain.entity.player.LineUpEntity;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AtBatProcessorTest {

    @Test
    @DisplayName("バント機会がなければバント戦略を呼び出さない")
    void doesNotCallBuntStrategyWithoutBuntOpportunity() {
        // given
        var batter = Mockito.mock(BatterEntity.class);
        var context = new GameBattingContext(new LineUpEntity(List.of(batter)));
        when(batter.swing(anyInt())).thenReturn(BattingResult.OUT);

        // when
        new AtBatProcessor().process(context, batter);

        // then
        assertAll(
                () -> verify(batter, never()).bunt(any()),
                () -> assertEquals(OutCount.ONE_OUT, context.getCurrentState().getOutCount()));
    }

    @Test
    @DisplayName("バント機会があればバント戦略を呼び出す")
    void callsBuntStrategyWhenBuntOpportunityExists() {
        // given
        var batter = Mockito.mock(BatterEntity.class);
        var context = new GameBattingContext(new LineUpEntity(List.of(batter)));
        context.hitSingle(batter);
        when(batter.bunt(any())).thenReturn(BuntResult.SUCCESS);
        when(batter.stealToDouble()).thenReturn(StealResult.NOT_TRY);

        // when
        new AtBatProcessor().process(context, batter);

        // then
        assertAll(
                () -> verify(batter).bunt(any()),
                () ->
                        assertEquals(
                                true,
                                context.getCurrentState()
                                        .isOccupied(
                                                com.example.baseballorders.simulator.domain.code
                                                        .Base.SECOND)),
                () -> assertEquals(OutCount.ONE_OUT, context.getCurrentState().getOutCount()));
    }

    @Test
    @DisplayName("一死でバント機会があればバント戦略を呼び出す")
    void callsBuntStrategyWithOneOutAndBuntOpportunity() {
        // given
        var batter = Mockito.mock(BatterEntity.class);
        var context = new GameBattingContext(new LineUpEntity(List.of(batter)));
        context.out();
        context.hitSingle(batter);
        when(batter.bunt(any())).thenReturn(BuntResult.SUCCESS);
        when(batter.stealToDouble()).thenReturn(StealResult.NOT_TRY);

        // when
        new AtBatProcessor().process(context, batter);

        // then
        assertAll(
                () -> verify(batter).bunt(any()),
                () -> assertEquals(OutCount.TWO_OUT, context.getCurrentState().getOutCount()));
    }
}

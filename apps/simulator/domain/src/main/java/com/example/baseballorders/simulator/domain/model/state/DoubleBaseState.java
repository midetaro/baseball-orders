package com.example.baseballorders.simulator.domain.model.state;

import com.example.baseballorders.simulator.domain.code.Base;
import com.example.baseballorders.simulator.domain.model.GameBattingContext;
import com.example.baseballorders.simulator.domain.model.player.BatterEntity;

public class DoubleBaseState implements BasesState, StealableToTripleBase {

    @Override
    public void hitSingle(GameBattingContext context, BatterEntity batterEntity) {
        context.moveRunnerNthBase(Base.FIRST);
        context.getRunners().setFirst(batterEntity);
    }

    @Override
    public void hitDouble(GameBattingContext context, BatterEntity batterEntity) {
        context.moveRunnerNthBase(Base.SECOND);
        context.getRunners().setSecond(batterEntity);
        context.addScore(1);
    }

    @Override
    public void hitTriple(GameBattingContext context, BatterEntity batterEntity) {
        context.moveRunnerNthBase(Base.THIRD);
        context.getRunners().setThird(batterEntity);
        context.addScore(1);
    }

    @Override
    public void hitHomer(GameBattingContext context, BatterEntity batterEntity) {
        context.moveRunnerNthBase(Base.THIRD);
        context.addScore(2);
    }
}

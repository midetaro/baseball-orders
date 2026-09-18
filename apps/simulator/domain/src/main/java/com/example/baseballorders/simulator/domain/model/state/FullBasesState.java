package com.example.baseballorders.simulator.domain.model.state;

import com.example.baseballorders.simulator.domain.code.Base;
import com.example.baseballorders.simulator.domain.model.GameBattingContext;
import com.example.baseballorders.simulator.domain.model.player.BatterEntity;

public final class FullBasesState extends BasesState {
    public FullBasesState() {
        this(null, null, null);
    }

    public FullBasesState(
            BatterEntity firstRunner, BatterEntity secondRunner, BatterEntity thirdRunner) {
        super(firstRunner, secondRunner, thirdRunner);
    }

    @Override
    public BasesState hitSingle(GameBattingContext context, BatterEntity batterEntity) {
        context.addScore(1);
        return advance(Base.FIRST).withRunnerAt(Base.FIRST, batterEntity);
    }

    @Override
    public BasesState hitDouble(GameBattingContext context, BatterEntity batterEntity) {
        context.addScore(2);
        return advance(Base.SECOND).withRunnerAt(Base.SECOND, batterEntity);
    }

    @Override
    public BasesState hitTriple(GameBattingContext context, BatterEntity batterEntity) {
        context.addScore(3);
        return advance(Base.THIRD).withRunnerAt(Base.THIRD, batterEntity);
    }

    @Override
    public BasesState hitHomer(GameBattingContext context, BatterEntity batterEntity) {
        context.addScore(4);
        return empty();
    }
}

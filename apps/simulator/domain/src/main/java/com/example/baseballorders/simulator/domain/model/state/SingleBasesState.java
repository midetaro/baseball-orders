package com.example.baseballorders.simulator.domain.model.state;

import com.example.baseballorders.simulator.domain.code.Base;
import com.example.baseballorders.simulator.domain.model.GameBattingContext;
import com.example.baseballorders.simulator.domain.model.player.BatterEntity;

public final class SingleBasesState extends BasesState implements StealableToDoubleBase {
    public SingleBasesState() { this(null); }
    public SingleBasesState(BatterEntity firstRunner) { super(firstRunner, null, null); }
    @Override public BatterEntity runnerOnFirst() { return runnerAt(Base.FIRST); }

    @Override
    public BasesState hitSingle(GameBattingContext context, BatterEntity batterEntity) {
        return advance(Base.FIRST).withRunnerAt(Base.FIRST, batterEntity);
    }

    @Override
    public BasesState hitDouble(GameBattingContext context, BatterEntity batterEntity) {
        return advance(Base.SECOND).withRunnerAt(Base.SECOND, batterEntity);
    }

    @Override
    public BasesState hitTriple(GameBattingContext context, BatterEntity batterEntity) {
        context.addScore(1);
        return advance(Base.THIRD).withRunnerAt(Base.THIRD, batterEntity);
    }

    @Override
    public BasesState hitHomer(GameBattingContext context, BatterEntity batterEntity) {
        context.addScore(2);
        return empty();
    }
}

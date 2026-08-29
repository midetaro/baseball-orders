package com.example.baseballorders.simulator.domain.model.state;

import com.example.baseballorders.simulator.domain.model.GameContext;
import com.example.baseballorders.simulator.domain.model.player.BatterEntity;

import java.util.Optional;

public class SingleBasesState implements BasesState, StealableToDoubleBase {

    @Override
    public void hitSingle(GameContext context, BatterEntity batterEntity) {
        context.moveRunnerNthBase(1);
        context.setRunnerOnFirstBase(Optional.of(batterEntity));
    }

    @Override
    public void hitDouble(GameContext context, BatterEntity batterEntity) {
        context.moveRunnerNthBase(2);
        context.setRunnerOnSecondBase(Optional.of(batterEntity));
    }

    @Override
    public void hitTriple(GameContext context, BatterEntity batterEntity) {
        context.moveRunnerNthBase(3);
        context.addScore(1);
        context.setRunnerOnThirdBase(Optional.of(batterEntity));
    }

    @Override
    public void hitHomer(GameContext context, BatterEntity batterEntity) {
        context.moveRunnerNthBase(4);
        context.addScore(2);
    }
}

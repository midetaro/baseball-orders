package com.example.baseballorders.simulator.domain.model.state;

import com.example.baseballorders.simulator.domain.model.GameContext;
import com.example.baseballorders.simulator.domain.model.player.BatterEntity;

import java.util.Optional;

public class NoBasesState implements BasesState {

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
        context.setRunnerOnThirdBase(Optional.of(batterEntity));
    }

    @Override
    public void hitHomer(GameContext context, BatterEntity batterEntity) {
        context.addScore(1);
    }
}

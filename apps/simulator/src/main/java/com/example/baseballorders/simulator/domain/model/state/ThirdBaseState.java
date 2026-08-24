package com.example.baseballorders.simulator.domain.model.state;

import com.example.baseballorders.simulator.domain.model.GameContext;
import com.example.baseballorders.simulator.domain.model.player.Batter;

import java.util.Optional;

public class ThirdBaseState implements BasesState {

    @Override
    public void hitSingle(GameContext context, Batter batter) {
        context.addScore(1);
        context.setRunnerOnFirstBase(Optional.of(batter));
    }

    @Override
    public void hitDouble(GameContext context, Batter batter) {
        context.moveRunnerNthBase(2);
        context.addScore(1);
        context.setRunnerOnSecondBase(Optional.of(batter));
    }

    @Override
    public void hitTriple(GameContext context, Batter batter) {
        context.moveRunnerNthBase(3);
        context.addScore(1);
        context.setRunnerOnThirdBase(Optional.of(batter));
    }

    @Override
    public void hitHomer(GameContext context, Batter batter) {
        context.moveRunnerNthBase(4);
        context.addScore(2);
    }
}

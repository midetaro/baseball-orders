package com.example.baseballorders.simulator.domain.model.state;

import com.example.baseballorders.simulator.domain.model.GameContext;
import com.example.baseballorders.simulator.domain.model.player.BatterEntity;

import java.util.Optional;

public class FullBasesState implements BasesState {

    @Override
    public void hitSingle(GameContext context, BatterEntity batterEntity) {
        context.setRunnerOnFirstBase(Optional.of(batterEntity));
        context.addScore(1);
    }

    @Override
    public void hitDouble(GameContext context, BatterEntity batterEntity) {
        context.moveRunnerNthBase(2);
        context.addScore(2);
        context.setRunnerOnSecondBase(Optional.of(batterEntity));

    }

    @Override
    public void hitTriple(GameContext context, BatterEntity batterEntity) {
        context.moveRunnerNthBase(3);
        context.addScore(3);
        context.setRunnerOnThirdBase(Optional.of(batterEntity));

    }

    @Override
    public void hitHomer(GameContext context, BatterEntity batterEntity) {
        context.moveRunnerNthBase(4);
        // 走者満塁の状態での処理
        context.addScore(4);

    }
}

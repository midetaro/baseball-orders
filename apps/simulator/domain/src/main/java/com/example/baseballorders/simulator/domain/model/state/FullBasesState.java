package com.example.baseballorders.simulator.domain.model.state;

import com.example.baseballorders.simulator.domain.code.Base;
import com.example.baseballorders.simulator.domain.model.GameContext;
import com.example.baseballorders.simulator.domain.model.player.BatterEntity;
import java.util.Optional;

public class FullBasesState implements BasesState {

    @Override
    public void hitSingle(GameContext context, BatterEntity batterEntity) {
        context.moveRunnerNthBase(Base.FIRST);
        context.setRunnerOnFirstBase(Optional.of(batterEntity));
        context.addScore(1);
    }

    @Override
    public void hitDouble(GameContext context, BatterEntity batterEntity) {
        context.moveRunnerNthBase(Base.SECOND);
        context.addScore(2);
        context.setRunnerOnSecondBase(Optional.of(batterEntity));
    }

    @Override
    public void hitTriple(GameContext context, BatterEntity batterEntity) {
        context.moveRunnerNthBase(Base.THIRD);
        context.addScore(3);
        context.setRunnerOnThirdBase(Optional.of(batterEntity));
    }

    @Override
    public void hitHomer(GameContext context, BatterEntity batterEntity) {
        context.moveRunnerNthBase(Base.THIRD);
        // 走者満塁の状態での処理
        context.addScore(4);
    }
}

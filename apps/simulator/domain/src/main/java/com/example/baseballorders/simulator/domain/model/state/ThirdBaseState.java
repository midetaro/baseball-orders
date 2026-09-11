package com.example.baseballorders.simulator.domain.model.state;

import com.example.baseballorders.simulator.domain.code.Base;
import com.example.baseballorders.simulator.domain.model.GameBattingContext;
import com.example.baseballorders.simulator.domain.model.player.BatterEntity;
import java.util.Optional;

public class ThirdBaseState implements BasesState {

    /**
     * Scores the runner on third and places the batter on first.
     *
     * @param context game context to update
     * @param batterEntity batter who hit the single
     */
    @Override
    public void hitSingle(GameBattingContext context, BatterEntity batterEntity) {
        context.moveRunnerNthBase(Base.FIRST);
        context.addScore(1);
        context.setRunnerOnFirstBase(Optional.of(batterEntity));
    }

    @Override
    public void hitDouble(GameBattingContext context, BatterEntity batterEntity) {
        context.moveRunnerNthBase(Base.SECOND);
        context.addScore(1);
        context.setRunnerOnSecondBase(Optional.of(batterEntity));
    }

    @Override
    public void hitTriple(GameBattingContext context, BatterEntity batterEntity) {
        context.moveRunnerNthBase(Base.THIRD);
        context.addScore(1);
        context.setRunnerOnThirdBase(Optional.of(batterEntity));
    }

    @Override
    public void hitHomer(GameBattingContext context, BatterEntity batterEntity) {
        context.moveRunnerNthBase(Base.THIRD);
        context.addScore(2);
    }
}

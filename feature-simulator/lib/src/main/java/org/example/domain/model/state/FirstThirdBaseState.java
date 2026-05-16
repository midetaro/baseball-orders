package org.example.domain.model.state;

import org.example.domain.model.GameContext;
import org.example.domain.model.player.Batter;

import java.util.Optional;

public class FirstThirdBaseState implements BasesState, StealableToDoubleBase {

    @Override
    public void hitSingle(GameContext context, Batter batter) {
        context.moveRunnerNthBase(1);
        context.setRunnerOnFirstBase(Optional.of(batter));
    }

    @Override
    public void hitDouble(GameContext context, Batter batter) {
        context.moveRunnerNthBase(2);
        context.setRunnerOnSecondBase(Optional.of(batter));
        context.addScore(1);
    }

    @Override
    public void hitTriple(GameContext context, Batter batter) {
        context.moveRunnerNthBase(3);
        context.setRunnerOnThirdBase(Optional.of(batter));
        context.addScore(2);

    }

    @Override
    public void hitHomer(GameContext context, Batter batter) {
        context.moveRunnerNthBase(4);
        context.addScore(3);

    }

}

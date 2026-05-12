package org.example.domain.state;

import org.example.domain.game.GameContext;
import org.example.domain.player.Batter;

public class DoubleThirdBaseState implements BasesState {

    @Override
    public void out(GameContext context, Batter batter) {
        context.addOut(1);
    }

    @Override
    public void singleHit(GameContext context, Batter batter) {
        context.addScore(1);
        context.updateState(new FirstThirdBaseState());
    }

    @Override
    public void hitDouble(GameContext context, Batter batter) {
        context.addScore(2);
        context.updateState(new DoubleBaseState());
    }

    @Override
    public void hitTriple(GameContext context, Batter batter) {
        context.addScore(2);
        context.updateState(new ThirdBaseState());
    }

    @Override
    public void hitHomer(GameContext context, Batter batter) {
        context.addScore(3);
        context.updateState(new NoBasesState());
    }
}

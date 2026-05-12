package org.example.domain.model.state;

import org.example.domain.model.GameContext;
import org.example.domain.model.player.Batter;

public class FirstDoubleBaseState implements BasesState {
    @Override
    public void out(GameContext context, Batter batter) {
        context.addOut(1);
    }

    @Override
    public void singleHit(GameContext context, Batter batter) {
        context.updateBaseState(new FullBasesState());
    }

    @Override
    public void hitDouble(GameContext context, Batter batter) {
        context.addScore(1);
        context.updateBaseState(new DoubleThirdBaseState());
    }

    @Override
    public void hitTriple(GameContext context, Batter batter) {
        context.addScore(2);
        context.updateBaseState(new ThirdBaseState());
    }

    @Override
    public void hitHomer(GameContext context, Batter batter) {
        context.addScore(3);
        context.updateBaseState(new NoBasesState());
    }
}

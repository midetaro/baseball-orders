package org.example.domain.model.state;

import org.example.domain.model.GameContext;
import org.example.domain.model.player.Batter;

public class DoubleThirdBaseState implements BasesState {

    @Override
    public void out(GameContext context, Batter batter) {
        context.addOutCounts(1);
    }

    @Override
    public void singleHit(GameContext context, Batter batter) {
        context.addScore(1);
        context.updateBaseState(new FirstThirdBaseState());
    }

    @Override
    public void hitDouble(GameContext context, Batter batter) {
        context.addScore(2);
        context.updateBaseState(new DoubleBaseState());
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

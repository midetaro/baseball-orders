package org.example.domain.state;

import org.example.domain.game.GameContext;
import org.example.domain.player.Batter;

public class SingleBasesState implements BasesState {

    @Override
    public void out(GameContext context, Batter batter) {
        context.addOut(1);
    }

    @Override
    public void singleHit(GameContext context, Batter batter) {
        context.updateBaseState(new FirstDoubleBaseState());
    }

    @Override
    public void hitDouble(GameContext context, Batter batter) {
        context.updateBaseState(new DoubleThirdBaseState());
    }

    @Override
    public void hitTriple(GameContext context, Batter batter) {
        context.addOut(1);
        context.updateBaseState(new ThirdBaseState());
    }

    @Override
    public void hitHomer(GameContext context, Batter batter) {
        context.addOut(2);
        context.updateBaseState(new NoBasesState());
    }
}

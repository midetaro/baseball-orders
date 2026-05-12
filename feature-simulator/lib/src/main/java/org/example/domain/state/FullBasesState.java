package org.example.domain.state;

import org.example.domain.game.GameContext;
import org.example.domain.player.Batter;

public class FullBasesState implements BasesState {

    @Override
    public void out(GameContext context, Batter batter) {
        context.addOut(1);
    }

    @Override
    public void singleHit(GameContext context, Batter batter) {
        context.addScore(1);
    }

    @Override
    public void hitDouble(GameContext context, Batter batter) {
        context.addScore(2);
        context.updateState(new FullBasesState());
    }

    @Override
    public void hitTriple(GameContext context, Batter batter) {
        context.addScore(3);
        context.updateState(new ThirdBaseState());
    }

    @Override
    public void hitHomer(GameContext context, Batter batter) {
        // 走者満塁の状態での処理
        context.addScore(4);
        context.updateState(new NoBasesState());
    }
}

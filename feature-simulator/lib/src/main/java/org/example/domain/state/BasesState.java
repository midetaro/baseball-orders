package org.example.domain.state;

import org.example.domain.game.GameContext;
import org.example.domain.player.Batter;

public interface BasesState {

    void out(GameContext context, Batter batter);

    void singleHit(GameContext context, Batter batter);

    void hitDouble(GameContext context, Batter batter);

    void hitTriple(GameContext context, Batter batter);

    void hitHomer(GameContext context, Batter batter);
}

package org.example.domain.model.state;

import org.example.domain.model.GameContext;
import org.example.domain.model.player.Batter;

public interface BasesState {

    void out(GameContext context, Batter batter);

    void singleHit(GameContext context, Batter batter);

    void hitDouble(GameContext context, Batter batter);

    void hitTriple(GameContext context, Batter batter);

    void hitHomer(GameContext context, Batter batter);
}

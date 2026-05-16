package org.example.domain.model.state;

import org.example.domain.model.GameContext;
import org.example.domain.model.player.Batter;

public interface BasesState {

    default void out(GameContext context) {
        context.addOutCounts(1);
    }

    void hitSingle(GameContext context, Batter batter);

    void hitDouble(GameContext context, Batter batter);

    void hitTriple(GameContext context, Batter batter);

    void hitHomer(GameContext context, Batter batter);
}

package com.example.baseballorders.simulator.domain.model.state;

import com.example.baseballorders.simulator.domain.model.GameContext;
import com.example.baseballorders.simulator.domain.model.player.Batter;

public interface BasesState {

    default void out(GameContext context) {
        context.addOutCounts(1);
    }

    void hitSingle(GameContext context, Batter batter);

    void hitDouble(GameContext context, Batter batter);

    void hitTriple(GameContext context, Batter batter);

    void hitHomer(GameContext context, Batter batter);
}

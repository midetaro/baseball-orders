package com.example.baseballorders.simulator.domain.model.state;

import com.example.baseballorders.simulator.domain.model.GameContext;
import com.example.baseballorders.simulator.domain.model.player.BatterEntity;

public interface BasesState {

    default void out(GameContext context) {
        context.addOutCounts(1);
    }

    void hitSingle(GameContext context, BatterEntity batterEntity);

    void hitDouble(GameContext context, BatterEntity batterEntity);

    void hitTriple(GameContext context, BatterEntity batterEntity);

    void hitHomer(GameContext context, BatterEntity batterEntity);
}

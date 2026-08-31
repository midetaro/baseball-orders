package com.example.baseballorders.simulator.domain.model.state;

import com.example.baseballorders.simulator.domain.model.GameBattingContext;
import com.example.baseballorders.simulator.domain.model.player.BatterEntity;

public interface BasesState {

    default void out(GameBattingContext context) {
        context.addOutCounts(1);
    }

    void hitSingle(GameBattingContext context, BatterEntity batterEntity);

    void hitDouble(GameBattingContext context, BatterEntity batterEntity);

    void hitTriple(GameBattingContext context, BatterEntity batterEntity);

    void hitHomer(GameBattingContext context, BatterEntity batterEntity);
}

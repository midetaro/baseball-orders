package com.example.baseballorders.simulator.domain.game;

import com.example.baseballorders.simulator.domain.play.OutCount;
import com.example.baseballorders.simulator.domain.player.BatterEntity;
import lombok.Getter;

/** 同一試合のStateが共有する、イニング単位のアウト数と走者。 */
public final class InningState {
    @Getter private OutCount outCount = OutCount.NO_OUT;
    private BatterEntity first;
    private BatterEntity second;
    private BatterEntity third;

    public BatterEntity runnerAt(Base base) {
        return switch (base) {
            case FIRST -> first;
            case SECOND -> second;
            case THIRD -> third;
        };
    }

    void place(BatterEntity first, BatterEntity second, BatterEntity third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    void addOut() {
        outCount = outCount.add(1);
    }

    void reset() {
        outCount = OutCount.NO_OUT;
        place(null, null, null);
    }
}

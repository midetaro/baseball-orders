package com.example.baseballorders.simulator.domain.model;

import com.example.baseballorders.simulator.domain.code.Base;
import com.example.baseballorders.simulator.domain.model.player.BatterEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/** 各塁にいる走者を表す値オブジェクト。走者がいない塁は {@code null} で表す。 */
@Getter
@Setter
@AllArgsConstructor
public final class BaseRunners {

    private BatterEntity first;
    private BatterEntity second;
    private BatterEntity third;

    public static BaseRunners empty() {
        return new BaseRunners(null, null, null);
    }

    void setRunner(Base base, BatterEntity runner) {
        switch (base) {
            case FIRST -> first = runner;
            case SECOND -> second = runner;
            case THIRD -> third = runner;
        }
    }

    BatterEntity runnerAt(Base base) {
        return switch (base) {
            case FIRST -> first;
            case SECOND -> second;
            case THIRD -> third;
        };
    }

    BaseRunners advance(Base bases) {
        return switch (bases) {
            case FIRST -> new BaseRunners(null, first, second);
            case SECOND -> new BaseRunners(null, null, first);
            case THIRD -> empty();
        };
    }

    int count() {
        return (first == null ? 0 : 1) + (second == null ? 0 : 1) + (third == null ? 0 : 1);
    }
}

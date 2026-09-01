package com.example.baseballorders.simulator.domain.code;

import lombok.Getter;

public enum Base {
    FIRST(1),
    SECOND(2),
    THIRD(3);

    /** The base number counted from home plate. */
    @Getter private final int number;

    Base(int number) {
        this.number = number;
    }
}

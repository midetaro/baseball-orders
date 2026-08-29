package com.example.baseballorders.simulator.domain.code;

public enum Base {
    FIRST(1),
    SECOND(2),
    THIRD(3);

    private final int number;

    Base(int number) {
        this.number = number;
    }

    public int getNumber() {
        return number;
    }
}

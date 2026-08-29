package com.example.baseballorders.simulator.domain.util;

public final class RandomGenerator {

    private RandomGenerator() {
    }

    public static float nextFloat() {
        return (float) Math.random();
    }
}

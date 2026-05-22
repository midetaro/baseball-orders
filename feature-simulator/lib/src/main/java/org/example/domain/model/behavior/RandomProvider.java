package org.example.domain.model.behavior;

final class RandomProvider {

    private RandomProvider() {
    }

    static float nextFloat() {
        return (float) Math.random();
    }
}

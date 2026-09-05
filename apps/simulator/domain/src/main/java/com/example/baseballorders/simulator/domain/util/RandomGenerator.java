package com.example.baseballorders.simulator.domain.util;

public final class RandomGenerator {

    private RandomGenerator() {}

    /**
     * 打撃や走塁などの確率判定に使用する乱数を生成する。
     *
     * @return Math.random() の結果を float に丸めた値（0.0 以上 1.0 以下）
     */
    public static float nextFloat() {
        return (float) Math.random();
    }
}

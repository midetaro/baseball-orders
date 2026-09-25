package com.example.baseballorders.simulator.domain.player.strategy;

import java.util.Arrays;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * {@link RandomGenerator#nextFloat()} を乱数列でスクリプト化し、決定論的なシナリオを書けるようにする。
 *
 * <p>シミュレーターの乱数源は {@code RandomGenerator.nextFloat()} だけなので、これを差し替えることで
 * 盗塁・バント・打撃・凡退進塁の全判定を脚本どおりに再現できる。
 *
 * <p>スクリプトが不足したら要求時点で失敗し、余ったら {@link #assertFullyConsumed()} で失敗する。
 * これにより本番の乱数消費回数が変わったときにシナリオが静かに壊れず、必ず赤くなる。
 */
public final class ScriptedRandom implements AutoCloseable {

    private final MockedStatic<RandomGenerator> mockedRandomGenerator;
    private final float[] script;
    private int consumed;

    private ScriptedRandom(float[] script) {
        this.script = script.clone();
        mockedRandomGenerator = Mockito.mockStatic(RandomGenerator.class);
        mockedRandomGenerator.when(RandomGenerator::nextFloat).thenAnswer(_ -> nextScriptedValue());
    }

    /**
     * 乱数列をスクリプト化した乱数源を作成する。
     *
     * @param script 先頭から順に返す乱数列
     * @return スクリプト化された乱数源
     */
    public static ScriptedRandom of(float... script) {
        return new ScriptedRandom(script);
    }

    /**
     * 同じ乱数を指定回数だけ返す乱数源を作成する。
     *
     * @param value 返す乱数
     * @param times 返す回数
     * @return スクリプト化された乱数源
     */
    public static ScriptedRandom repeating(float value, int times) {
        return new ScriptedRandom(repeat(value, times));
    }

    /**
     * 乱数列を連結する。
     *
     * @param parts 連結する乱数列
     * @return 連結された乱数列
     */
    public static float[] concat(float[]... parts) {
        int length = Arrays.stream(parts).mapToInt(part -> part.length).sum();
        float[] script = new float[length];
        int offset = 0;
        for (float[] part : parts) {
            System.arraycopy(part, 0, script, offset, part.length);
            offset += part.length;
        }
        return script;
    }

    /**
     * 同じ乱数を指定回数だけ並べた乱数列を作成する。
     *
     * @param value 並べる乱数
     * @param times 並べる回数
     * @return 乱数列
     */
    public static float[] repeat(float value, int times) {
        float[] script = new float[times];
        Arrays.fill(script, value);
        return script;
    }

    /**
     * 本番コードが消費した乱数の個数を返す。
     *
     * @return 消費された乱数の個数
     */
    public int consumedCount() {
        return consumed;
    }

    /** スクリプトを最後まで消費したことを検証する。 */
    public void assertFullyConsumed() {
        if (consumed != script.length) {
            throw new AssertionError(
                    "乱数スクリプトが余った: " + script.length + "個のうち" + consumed + "個しか消費されていない");
        }
    }

    @Override
    public void close() {
        mockedRandomGenerator.close();
    }

    private float nextScriptedValue() {
        if (consumed >= script.length) {
            throw new AssertionError("乱数スクリプトが尽きた: " + (consumed + 1) + "回目の要求");
        }
        return script[consumed++];
    }
}

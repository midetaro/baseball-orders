package com.example.baseballorders.backend.domain;

import java.util.Objects;
import org.jilt.Builder;
import org.jilt.BuilderStyle;

/** シミュレーションに使用する選手の打撃データ。 */
@Builder(style = BuilderStyle.STAGED)
public record PlayerData(
        String name,
        float hitAverage,
        float sluggish,
        float buntSuccessRate,
        boolean buntEnabled,
        float stealSuccessRate,
        boolean stealEnabled,
        PlayerPersonality personality) {

    private static final float MIN_HIT_AVERAGE = 0.005f;
    private static final float MAX_HIT_AVERAGE = 0.400f;
    private static final float MIN_SLUGGISH = 0.100f;
    private static final float MAX_SLUGGISH = 0.600f;
    private static final float MIN_STEAL_SUCCESS_RATE = 0.100f;
    private static final float MAX_STEAL_SUCCESS_RATE = 0.900f;

    /** 入力された打撃データがシミュレーション可能な範囲であることを検証する。 */
    public PlayerData {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(personality, "personality must not be null");
        requireRange(hitAverage, MIN_HIT_AVERAGE, MAX_HIT_AVERAGE, "hitAverage");
        requireRange(sluggish, MIN_SLUGGISH, MAX_SLUGGISH, "sluggish");
        requireRange(
                stealSuccessRate,
                MIN_STEAL_SUCCESS_RATE,
                MAX_STEAL_SUCCESS_RATE,
                "stealSuccessRate");
    }

    /**
     * Creates player data with the default personality for compatibility with existing callers.
     *
     * @param name 選手名
     * @param hitAverage 打率
     * @param sluggish 長打率
     * @param buntSuccessRate バント成功率
     * @param buntEnabled バントを試みるかどうか
     * @param stealSuccessRate 盗塁成功率
     * @param stealEnabled 盗塁を試みるかどうか
     */
    public PlayerData(
            String name,
            float hitAverage,
            float sluggish,
            float buntSuccessRate,
            boolean buntEnabled,
            float stealSuccessRate,
            boolean stealEnabled) {
        this(
                name,
                hitAverage,
                sluggish,
                buntSuccessRate,
                buntEnabled,
                stealSuccessRate,
                stealEnabled,
                PlayerPersonality.DEFAULT);
    }

    private static void requireRange(float value, float minimum, float maximum, String name) {
        if (Float.isNaN(value) || value < minimum || value > maximum) {
            throw new IllegalArgumentException(
                    name + " must be between " + minimum + " and " + maximum);
        }
    }
}

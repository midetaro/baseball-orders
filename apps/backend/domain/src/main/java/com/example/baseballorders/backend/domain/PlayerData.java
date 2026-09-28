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
        boolean buntEnabled,
        boolean stealEnabled,
        PlayerPersonality personality) {

    // 以下の境界値は「確率は0.0以上1.0以下」というdomainの固定ルールを表すため、
    // 設定ファイルへは移動しない。
    private static final float MIN_HIT_AVERAGE = 0.000f;
    private static final float MAX_HIT_AVERAGE = 1.000f;
    private static final float MIN_SLUGGISH = 0.000f;
    private static final float MAX_SLUGGISH = 1.000f;

    /** 入力された打撃データが確率として成立する範囲であることを検証する。 */
    public PlayerData {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(personality, "personality must not be null");
        requireRange(hitAverage, MIN_HIT_AVERAGE, MAX_HIT_AVERAGE, "hitAverage");
        requireRange(sluggish, MIN_SLUGGISH, MAX_SLUGGISH, "sluggish");
    }

    /**
     * Creates player data with the default personality for compatibility with existing callers.
     *
     * @param name 選手名
     * @param hitAverage 打率
     * @param sluggish 長打率
     * @param buntEnabled バントを試みるかどうか
     * @param stealEnabled 盗塁を試みるかどうか
     */
    public PlayerData(
            String name,
            float hitAverage,
            float sluggish,
            boolean buntEnabled,
            boolean stealEnabled) {
        this(name, hitAverage, sluggish, buntEnabled, stealEnabled, PlayerPersonality.DEFAULT);
    }

    private static void requireRange(float value, float minimum, float maximum, String name) {
        if (Float.isNaN(value) || value < minimum || value > maximum) {
            throw new IllegalArgumentException(
                    name + " must be between " + minimum + " and " + maximum);
        }
    }
}

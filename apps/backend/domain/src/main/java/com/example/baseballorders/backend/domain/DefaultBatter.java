package com.example.baseballorders.backend.domain;

import java.util.Objects;
import org.jilt.Builder;
import org.jilt.BuilderStyle;

/**
 * チームごとの既定オーダーに含まれる打者。
 *
 * <p>stealForcedがtrueの打者は常に盗塁し、盗塁の有無を選択できない。buntForcedも同様にバントを表す。
 */
@Builder(style = BuilderStyle.STAGED)
public record DefaultBatter(
        int battingOrder,
        float hitAverage,
        PlayerPersonality personality,
        boolean stealForced,
        boolean buntForced) {

    // 打順1〜9と確率0.0〜1.0は野球・確率の固定ルールであり設定化しない。
    private static final int MIN_BATTING_ORDER = 1;
    private static final int MAX_BATTING_ORDER = 9;
    private static final float MIN_HIT_AVERAGE = 0.0f;
    private static final float MAX_HIT_AVERAGE = 1.0f;

    /**
     * 打順・打率・性格を検証する。
     *
     * @throws IllegalArgumentException 打順または打率が範囲外の場合
     * @throws NullPointerException personalityがnullの場合
     */
    public DefaultBatter {
        if (battingOrder < MIN_BATTING_ORDER || battingOrder > MAX_BATTING_ORDER) {
            throw new IllegalArgumentException("battingOrder must be between 1 and 9");
        }
        if (Float.isNaN(hitAverage)
                || hitAverage < MIN_HIT_AVERAGE
                || hitAverage > MAX_HIT_AVERAGE) {
            throw new IllegalArgumentException("hitAverage must be between 0.0 and 1.0");
        }
        Objects.requireNonNull(personality, "personality must not be null");
    }
}

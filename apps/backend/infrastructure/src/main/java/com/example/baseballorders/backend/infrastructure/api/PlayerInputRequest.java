package com.example.baseballorders.backend.infrastructure.api;

import com.example.baseballorders.backend.domain.PlayerPersonality;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import org.jilt.Builder;
import org.jilt.BuilderStyle;

/** シミュレーション画面から受け取る1打順分の入力値。 */
@Builder(style = BuilderStyle.STAGED)
public record PlayerInputRequest(
        @JsonProperty("hit_average") Float hitAverage,
        Float sluggish,
        @JsonProperty("bunt_success_rate") Float buntSuccessRate,
        @JsonProperty("steal_success_rate") Float stealSuccessRate,
        @JsonProperty("bunt_enabled") Boolean buntEnabled,
        @JsonProperty("steal_enabled") Boolean stealEnabled,
        PlayerPersonality personality) {

    /** 必須の画面入力が欠けていないことを検証する。 */
    public PlayerInputRequest {
        Objects.requireNonNull(hitAverage, "hit_average must not be null");
        Objects.requireNonNull(sluggish, "sluggish must not be null");
        Objects.requireNonNull(buntSuccessRate, "bunt_success_rate must not be null");
        Objects.requireNonNull(stealSuccessRate, "steal_success_rate must not be null");
        Objects.requireNonNull(buntEnabled, "bunt_enabled must not be null");
        Objects.requireNonNull(stealEnabled, "steal_enabled must not be null");
        personality = personality == null ? PlayerPersonality.DEFAULT : personality;
    }

    /**
     * Creates an input request with the default personality for existing API callers.
     *
     * @param hitAverage 出塁率
     * @param sluggish 長打率
     * @param buntSuccessRate バント成功率
     * @param stealSuccessRate 盗塁成功率
     * @param buntEnabled バントを試みるかどうか
     * @param stealEnabled 盗塁を試みるかどうか
     */
    public PlayerInputRequest(
            Float hitAverage,
            Float sluggish,
            Float buntSuccessRate,
            Float stealSuccessRate,
            Boolean buntEnabled,
            Boolean stealEnabled) {
        this(
                hitAverage,
                sluggish,
                buntSuccessRate,
                stealSuccessRate,
                buntEnabled,
                stealEnabled,
                PlayerPersonality.DEFAULT);
    }
}

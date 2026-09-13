package com.example.baseballorders.backend.infrastructure.api;

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
        @JsonProperty("steal_enabled") Boolean stealEnabled) {

    /** 必須の画面入力が欠けていないことを検証する。 */
    public PlayerInputRequest {
        Objects.requireNonNull(hitAverage, "hit_average must not be null");
        Objects.requireNonNull(sluggish, "sluggish must not be null");
        Objects.requireNonNull(buntSuccessRate, "bunt_success_rate must not be null");
        Objects.requireNonNull(stealSuccessRate, "steal_success_rate must not be null");
        Objects.requireNonNull(buntEnabled, "bunt_enabled must not be null");
        Objects.requireNonNull(stealEnabled, "steal_enabled must not be null");
    }
}

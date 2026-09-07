package com.example.baseballorders.backend.infrastructure.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * シミュレーションAPIで受け取る選手識別子。
 *
 * @param playerId 選手を識別するID
 * @param buntEnabled バントを試みるかどうか
 */
public record PlayerIdRequest(
        @JsonProperty("player_id") Long playerId,
        @JsonProperty("bunt_enabled") Boolean buntEnabled) {

    public PlayerIdRequest {
        Objects.requireNonNull(buntEnabled, "bunt_enabled must not be null");
    }
}

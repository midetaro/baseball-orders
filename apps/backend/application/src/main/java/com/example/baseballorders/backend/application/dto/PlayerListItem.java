package com.example.baseballorders.backend.application.dto;

/** シミュレーション画面に表示する選手情報。 */
public record PlayerListItem(
        Long playerId,
        String name,
        float hitAverage,
        float sluggish,
        float buntSuccessRate,
        float stealSuccessRate) {}

package com.example.baseballorders.backend.domain;

/** シミュレーションに使用する選手の打撃データ。 */
public record PlayerData(
        String name,
        float hitAverage,
        float sluggish,
        float buntSuccessRate,
        float stealSuccessRate) {}

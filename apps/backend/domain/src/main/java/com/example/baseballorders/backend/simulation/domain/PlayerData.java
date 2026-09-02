package com.example.baseballorders.backend.simulation.domain;

/** シミュレーションに使用する選手の打撃データ。 */
public record PlayerData(String name, float hitAverage, float sluggish) {}

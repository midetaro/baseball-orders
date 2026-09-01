package com.example.baseballorders.messaging;

/**
 * シミュレーション要求で共有する選手データ。
 *
 * @param name 選手名
 * @param hitAverage 打率
 * @param sluggish 長打率
 */
public record SimulationPlayerMessage(String name, float hitAverage, float sluggish) {}

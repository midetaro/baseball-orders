package com.example.baseballorders.backend.simulation.infrastructure.messaging;

/**
 * APIから受け取り、simulatorへ送信する選手の打撃データ。
 *
 * @param name 選手名
 * @param hitAverage 打率
 * @param sluggish 長打率
 */
public record PlayerData(String name, float hitAverage, float sluggish) {}

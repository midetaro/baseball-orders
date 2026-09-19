package com.example.baseballorders.simulator.domain.model.state;

/** 塁上プレーを適用した結果の塁状態と得点。 */
public record BaseTransition(BasesState nextState, long scoredRuns) {}

package com.example.baseballorders.backend.application.dto;

/** シミュレーション要求に含める選手と、その打席でバントを試みる選択。 */
public record SimulationPlayerSelection(Long playerId, boolean buntEnabled) {}

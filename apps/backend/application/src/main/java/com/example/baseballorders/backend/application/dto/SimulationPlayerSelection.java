package com.example.baseballorders.backend.application.dto;

import org.jilt.Builder;
import org.jilt.BuilderStyle;

/** シミュレーション要求に含める選手と、その打席でバントを試みる選択。 */
@Builder(style = BuilderStyle.STAGED)
public record SimulationPlayerSelection(Long playerId, boolean buntEnabled) {}

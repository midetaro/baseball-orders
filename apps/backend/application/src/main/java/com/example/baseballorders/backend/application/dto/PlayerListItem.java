package com.example.baseballorders.backend.application.dto;

import org.jilt.Builder;
import org.jilt.BuilderStyle;

/** シミュレーション画面に表示する選手情報。 */
@Builder(style = BuilderStyle.STAGED)
public record PlayerListItem(
        Long playerId,
        String name,
        float hitAverage,
        float sluggish,
        float buntSuccessRate,
        float stealSuccessRate) {}

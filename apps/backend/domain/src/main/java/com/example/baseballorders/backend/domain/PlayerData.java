package com.example.baseballorders.backend.domain;

import org.jilt.Builder;
import org.jilt.BuilderStyle;

/** シミュレーションに使用する選手の打撃データ。 */
@Builder(style = BuilderStyle.STAGED)
public record PlayerData(
        String name,
        float hitAverage,
        float sluggish,
        float buntSuccessRate,
        boolean buntEnabled,
        float stealSuccessRate) {}

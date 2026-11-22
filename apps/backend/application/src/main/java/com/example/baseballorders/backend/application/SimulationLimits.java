package com.example.baseballorders.backend.application;

import org.jilt.Builder;
import org.jilt.BuilderStyle;

/**
 * シミュレーション受付の調整可能な上限値。
 *
 * <p>すべての値は設定ファイル(application.yml)から注入される運用パラメータであり、コード上の既定値を持たない。
 *
 * @param maximumAverageHitAverage 打順9人の打率平均の上限
 */
@Builder(style = BuilderStyle.STAGED)
public record SimulationLimits(float maximumAverageHitAverage) {}

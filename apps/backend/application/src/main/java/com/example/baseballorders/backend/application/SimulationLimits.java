package com.example.baseballorders.backend.application;

import java.time.Duration;
import java.util.Objects;
import org.jilt.Builder;
import org.jilt.BuilderStyle;

/**
 * シミュレーション受付の調整可能な上限値。
 *
 * <p>すべての値は設定ファイル(application.yml)から注入される運用パラメータであり、コード上の既定値を持たない。
 */
@Builder(style = BuilderStyle.STAGED)
public record SimulationLimits(
        Duration resultTimeout,
        float maximumAverageHitAverage,
        float maximumAverageSluggish,
        float pitcherIncreaseMultiplier,
        float maximumSuccessRate) {

    /**
     * 結果待機時間が指定されていることだけを検証して上限値を生成する。
     *
     * @throws NullPointerException resultTimeoutがnullの場合
     */
    public SimulationLimits {
        Objects.requireNonNull(resultTimeout, "resultTimeout must not be null");
    }
}

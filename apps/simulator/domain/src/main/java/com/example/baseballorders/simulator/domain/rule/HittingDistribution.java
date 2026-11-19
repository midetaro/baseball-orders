package com.example.baseballorders.simulator.domain.rule;

import org.jilt.Builder;
import org.jilt.BuilderStyle;

/**
 * 安打を単打・二塁打・三塁打・本塁打へ配分する相対的な重み設定。
 *
 * <p>安打になる確率そのものは従来どおり {@code (1 - 四球確率) * 打率} で決まり、この重みは安打を各安打種別へ「重み /
 * 重みの合計」の割合で振り分ける。重みは相対値であり、合計が 1 である必要はない。重みが0の安打種別は発生しない。
 *
 * @param singleWeight 単打の重み
 * @param doubleWeight 二塁打の重み
 * @param tripleWeight 三塁打の重み
 * @param homeRunWeight 本塁打の重み
 */
@Builder(style = BuilderStyle.STAGED)
public record HittingDistribution(
        float singleWeight, float doubleWeight, float tripleWeight, float homeRunWeight) {}

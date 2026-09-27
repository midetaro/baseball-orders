package com.example.baseballorders.simulator.domain.rule;

import org.jilt.Builder;
import org.jilt.BuilderStyle;

/**
 * 打席結果の判定に使う、打者の成績に依存しない確率設定。
 *
 * @param walkProbability 四球となる確率の上限。出塁率がこれを下回る打者では出塁率が上限になる
 * @param strikeoutProbabilityWhenNotOnBase 出塁しなかった打席のうち三振になる割合
 */
@Builder(style = BuilderStyle.STAGED)
public record BattingProbabilities(
        float walkProbability, float strikeoutProbabilityWhenNotOnBase) {}

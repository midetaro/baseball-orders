package com.example.baseballorders.simulator.domain.model.state;

import com.example.baseballorders.simulator.domain.model.GameBattingContext;
import com.example.baseballorders.simulator.domain.model.player.BatterEntity;

public interface BasesState {

    /**
     * アウトとなった打撃結果を適用し、アウト数を一つ増やす。
     *
     * @param context 更新対象の試合状態
     */
    default void out(GameBattingContext context) {
        context.addOutCounts(1);
    }

    /**
     * 単打による走者の進塁と得点を適用し、打者を一塁に置く。
     *
     * @param context 更新対象の試合状態
     * @param batterEntity 単打を打った打者
     */
    void hitSingle(GameBattingContext context, BatterEntity batterEntity);

    /**
     * 二塁打による走者の進塁と得点を適用し、打者を二塁に置く。
     *
     * @param context 更新対象の試合状態
     * @param batterEntity 二塁打を打った打者
     */
    void hitDouble(GameBattingContext context, BatterEntity batterEntity);

    /**
     * 三塁打による走者の生還と得点を適用し、打者を三塁に置く。
     *
     * @param context 更新対象の試合状態
     * @param batterEntity 三塁打を打った打者
     */
    void hitTriple(GameBattingContext context, BatterEntity batterEntity);

    /**
     * 本塁打による走者と打者の得点を加算し、塁上の走者を取り除く。
     *
     * @param context 更新対象の試合状態
     * @param batterEntity 本塁打を打った打者
     */
    void hitHomer(GameBattingContext context, BatterEntity batterEntity);
}

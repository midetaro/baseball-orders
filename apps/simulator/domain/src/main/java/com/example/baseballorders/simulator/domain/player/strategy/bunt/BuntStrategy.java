package com.example.baseballorders.simulator.domain.player.strategy.bunt;

import com.example.baseballorders.simulator.domain.play.BuntResult;
import com.example.baseballorders.simulator.domain.play.BuntType;
import com.example.baseballorders.simulator.domain.play.OutCount;

/** バント戦略。 */
public sealed interface BuntStrategy
        permits EagerBuntStrategy, NowayBuntStrategy, StandardBuntStrategy {

    /**
     * 試合状況に応じて、バント種別ごとに設定された成功率でバントを試みる。
     *
     * @param outCount アウトカウント
     * @param buntType バント種別
     * @return バント結果
     */
    BuntResult bunt(OutCount outCount, BuntType buntType);
}

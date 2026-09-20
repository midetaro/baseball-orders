package com.example.baseballorders.simulator.domain.entity.behavior.bunt;

import com.example.baseballorders.simulator.domain.code.BuntResult;
import com.example.baseballorders.simulator.domain.code.OutCount;

/** バント戦略。 */
public sealed interface BuntStrategy
        permits EagerBuntStrategy, NowayBuntStrategy, StandardBuntStrategy {

    /**
     * 試合状況に応じて、指定された成功率でバントを試みる。
     *
     * @param successRate バント成功率
     * @param outCount アウトカウント
     * @return バント結果
     */
    BuntResult bunt(float successRate, OutCount outCount);
}

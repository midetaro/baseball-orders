package com.example.baseballorders.simulator.domain.model.behavior;

import com.example.baseballorders.simulator.domain.code.BuntResult;
import com.example.baseballorders.simulator.domain.code.OutCount;
import com.example.baseballorders.simulator.domain.model.state.BasesState;

/** バント戦略。 */
public interface BuntStrategy {

    /**
     * 試合状況に応じて、指定された成功率でバントを試みる。
     *
     * @param successRate バント成功率
     * @param outCount アウトカウント
     * @param basesState 現在の塁状態
     * @return バント結果
     */
    BuntResult bunt(float successRate, OutCount outCount, BasesState basesState);
}

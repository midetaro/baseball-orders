package com.example.baseballorders.simulator.domain.model.behavior;

import com.example.baseballorders.simulator.domain.code.BuntResult;

/** バント戦略。 */
public interface BuntStrategy {

    /**
     * 指定された成功率でバントを試みる。
     *
     * @param successRate バント成功率
     * @return バント結果
     */
    BuntResult bunt(float successRate);
}

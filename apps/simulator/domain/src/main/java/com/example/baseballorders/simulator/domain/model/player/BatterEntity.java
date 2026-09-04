package com.example.baseballorders.simulator.domain.model.player;

import com.example.baseballorders.simulator.domain.code.BattingResult;
import com.example.baseballorders.simulator.domain.code.BuntResult;
import com.example.baseballorders.simulator.domain.code.StealResult;
import com.example.baseballorders.simulator.domain.model.behavior.AtBatBehavior;
import com.example.baseballorders.simulator.domain.model.behavior.BuntStrategy;
import com.example.baseballorders.simulator.domain.model.behavior.StealStrategy;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** 打者 */
@Getter
@AllArgsConstructor
public class BatterEntity extends Player {

    /** 選手名 */
    private final String name;

    /** 打率 */
    private final float hitAverage;

    /** 長打率 */
    private final float sluggish;

    /** バント成功率 */
    private final float buntSuccessRate;

    /** 打撃戦略 */
    private final AtBatBehavior atBatBehavior;

    /** 走塁戦略 */
    private final StealStrategy stealStrategy;

    /** バント戦略 */
    private final BuntStrategy buntStrategy;

    /**
     * 打撃戦略に従って打撃する。
     *
     * @return 打席結果
     */
    public BattingResult swing() {
        return atBatBehavior.batting(this.hitAverage, this.sluggish);
    }

    /**
     * 二塁への盗塁を試みる。
     *
     * @return 盗塁結果
     */
    public StealResult stealToDouble() {
        return stealStrategy.runToDouble();
    }

    /**
     * 三塁への盗塁を試みる。
     *
     * @return 盗塁結果
     */
    public StealResult stealToTriple() {
        return stealStrategy.runToTriple();
    }

    /**
     * バント戦略に従ってバントする。
     *
     * @return バント結果
     */
    public BuntResult bunt() {
        return buntStrategy.bunt(buntSuccessRate);
    }
}

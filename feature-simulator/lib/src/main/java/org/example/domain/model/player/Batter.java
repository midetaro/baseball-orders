package org.example.domain.model.player;

import org.example.domain.code.BattingResult;
import org.example.domain.code.StealResult;
import org.example.domain.model.behavior.AtBatBehavior;
import org.example.domain.model.behavior.StealStrategy;

/**
 * 打者
 */
public class Batter extends Player {

    /**
     * 打率
     */
    private final float hitAverage;

    /**
     * 長打率
     */
    private final float sluggish;

    /**
     * 打撃戦略
     */
    private final AtBatBehavior atBatBehavior;

    /**
     * 走塁戦略
     */
    private final StealStrategy stealStrategy;

    public Batter(String name, float hitAverage, float sluggish, AtBatBehavior atBatBehavior, StealStrategy stealStrategy) {
        this.sluggish = sluggish;
        this.name = name;
        this.hitAverage = hitAverage;
        this.atBatBehavior = atBatBehavior;
        this.stealStrategy = stealStrategy;
    }

    public BattingResult swing() {
        return atBatBehavior.batting(this.hitAverage, this.sluggish);
    }

    public StealResult stealToDouble() {
        return stealStrategy.runToDouble();
    }

    public StealResult stealToTriple() {
        return stealStrategy.runToTriple();
    }
}

package org.example.domain.model.player;

import org.example.domain.code.BattingResult;
import org.example.domain.model.behavior.AtBatBehavior;

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

    private AtBatBehavior atBatBehavior;

    public Batter(String name, float hitAverage, float sluggish) {
        this.sluggish = sluggish;
        this.name = name;
        this.hitAverage = hitAverage;
    }

    public BattingResult swing() {
        return atBatBehavior.batting(this.hitAverage, this.sluggish);
    }
}

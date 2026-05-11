package org.example.domain.player;

import org.example.domain.behavior.BattingBehavior;
import org.example.domain.code.BattingResult;

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

    private BattingBehavior battingBehavior;

    public Batter(String name, float hitAverage, float sluggish) {
        this.sluggish = sluggish;
        this.name = name;
        this.hitAverage = hitAverage;
    }

    public BattingResult swing() {
        return battingBehavior.batting(this.hitAverage, this.sluggish);
    }
}

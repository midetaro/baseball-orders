package org.example.domain.mock;

import org.example.domain.model.behavior.ShortDistanceAtBatBehavior;
import org.example.domain.model.player.Batter;

import java.util.List;

public class Mock_Batters {

    static ShortDistanceAtBatBehavior shortDistanceAtBatBehavior = new ShortDistanceAtBatBehavior();

    public static List<Batter> mock() {
        return List.of(
                new Batter("batter1", 0.3f, 0.4f, shortDistanceAtBatBehavior),
                new Batter("batter2", 0.4f, 0.5f, shortDistanceAtBatBehavior),
                new Batter("batter3", 0.5f, 0.6f, shortDistanceAtBatBehavior),
                new Batter("batter1", 0.3f, 0.6f, shortDistanceAtBatBehavior),
                new Batter("batter2", 0.4f, 0.5f, shortDistanceAtBatBehavior),
                new Batter("batter3", 0.5f, 0.3f, shortDistanceAtBatBehavior),
                new Batter("batter1", 0.3f, 0.6f, shortDistanceAtBatBehavior),
                new Batter("batter2", 0.4f, 0.4f, shortDistanceAtBatBehavior),
                new Batter("batter3", 0.5f, 0.5f, shortDistanceAtBatBehavior)
        );
    }
}

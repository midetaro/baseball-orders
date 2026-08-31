package com.example.baseballorders.simulator.application.mapper;

import com.example.baseballorders.simulator.application.dto.PlayerData;
import com.example.baseballorders.simulator.domain.model.behavior.AtBatBehavior;
import com.example.baseballorders.simulator.domain.model.behavior.StealStrategy;
import com.example.baseballorders.simulator.domain.model.player.BatterEntity;
import com.example.baseballorders.simulator.domain.model.player.LineUpEntity;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/** Maps player data received by the application to the simulation domain model. */
@Component
public class LineUpMapper {

    private final AtBatBehavior atBatBehavior;
    private final StealStrategy stealStrategy;

    /**
     * Creates a mapper using the default batting and stealing strategies.
     *
     * @param atBatBehavior behavior assigned to each batter
     * @param stealStrategy stealing strategy assigned to each batter
     */
    public LineUpMapper(
            @Qualifier("shortDistanceAtBat") AtBatBehavior atBatBehavior,
            @Qualifier("nowayStealBehavior") StealStrategy stealStrategy) {
        this.atBatBehavior = atBatBehavior;
        this.stealStrategy = stealStrategy;
    }

    /**
     * Converts SQS player data to a domain lineup.
     *
     * @param players players contained in a simulation request
     * @return lineup containing mapped batter entities in request order
     */
    public LineUpEntity map(List<PlayerData> players) {
        List<BatterEntity> batters =
                players.stream()
                        .map(
                                player ->
                                        new BatterEntity(
                                                player.name(),
                                                player.hitAverage(),
                                                player.slugging(),
                                                atBatBehavior,
                                                stealStrategy))
                        .toList();
        return new LineUpEntity(batters);
    }
}

package com.example.baseballorders.simulator.application;

import com.example.baseballorders.messaging.PlayerPersonality;
import com.example.baseballorders.messaging.SimulationPlayerMessage;
import com.example.baseballorders.simulator.domain.entity.behavior.BehaviorStrategies;
import com.example.baseballorders.simulator.domain.entity.behavior.batting.HittingStrategy;
import com.example.baseballorders.simulator.domain.entity.behavior.bunt.BuntStrategy;
import com.example.baseballorders.simulator.domain.entity.behavior.steal.StealStrategy;
import com.example.baseballorders.simulator.domain.entity.player.BatterEntity;
import com.example.baseballorders.simulator.domain.entity.player.LineUpEntity;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/** Maps player data received by the application to the simulation domain model. */
@Component
public class LineUpMapper {

    private final HittingStrategy hittingStrategy;
    private final StealStrategy eagerStealStrategy;
    private final BuntStrategy buntStrategy;
    private final StealStrategy noStealStrategy = BehaviorStrategies.noSteal();
    private final BuntStrategy noBuntStrategy = BehaviorStrategies.noBunt();

    /**
     * Creates a mapper using the default batting and stealing strategies.
     *
     * @param hittingStrategy middle-distance batting behavior assigned to each batter
     * @param stealStrategy stealing strategy assigned to each batter
     * @param buntStrategy bunt strategy assigned to each batter
     */
    public LineUpMapper(
            @Qualifier("middleDistanceAtBat") HittingStrategy hittingStrategy,
            @Qualifier("eagerStealBehavior") StealStrategy stealStrategy,
            @Qualifier("standardBuntStrategy") BuntStrategy buntStrategy) {
        this.hittingStrategy = hittingStrategy;
        this.eagerStealStrategy = stealStrategy;
        this.buntStrategy = buntStrategy;
    }

    /**
     * Converts SQS player data to a domain lineup, disabling steals and bunts according to each
     * player's selection. The legacy {@code hitAverage} wire field is interpreted as the domain
     * on-base percentage to retain the existing message contract.
     *
     * @param players players contained in a simulation request
     * @return lineup containing mapped batter entities in request order
     */
    public LineUpEntity map(List<SimulationPlayerMessage> players) {
        List<BatterEntity> batters =
                players.stream()
                        .map(
                                player ->
                                        new BatterEntity(
                                                player.hitAverage(),
                                                player.sluggish(),
                                                player.buntSuccessRate(),
                                                player.stealSuccessRate(),
                                                atBatBehaviorFor(player.personality()),
                                                player.stealEnabled()
                                                        ? stealStrategyFor(player.personality())
                                                        : noStealStrategy,
                                                player.buntEnabled()
                                                        ? buntStrategyFor(player.personality())
                                                        : noBuntStrategy))
                        .toList();
        return new LineUpEntity(batters);
    }

    private HittingStrategy atBatBehaviorFor(PlayerPersonality personality) {
        return switch (personality) {
            case DEFAULT, EAGER_STEAL, EAGER_BUNT -> hittingStrategy;
            case EAGER_SLUGGISH -> BehaviorStrategies.longDistanceAtBat();
        };
    }

    private StealStrategy stealStrategyFor(PlayerPersonality personality) {
        return switch (personality) {
            case DEFAULT, EAGER_SLUGGISH, EAGER_STEAL, EAGER_BUNT -> eagerStealStrategy;
        };
    }

    private BuntStrategy buntStrategyFor(PlayerPersonality personality) {
        return switch (personality) {
            case DEFAULT, EAGER_SLUGGISH, EAGER_STEAL -> buntStrategy;
            case EAGER_BUNT -> BehaviorStrategies.eagerBunt();
        };
    }
}

package com.example.baseballorders.simulator.infrastructure.messaging;

import com.example.baseballorders.messaging.PlayerPersonality;
import com.example.baseballorders.messaging.SimulationPlayerMessage;
import com.example.baseballorders.simulator.domain.player.BatterEntity;
import com.example.baseballorders.simulator.domain.player.LineUpEntity;
import com.example.baseballorders.simulator.domain.player.strategy.BehaviorStrategies;
import com.example.baseballorders.simulator.domain.player.strategy.batting.HittingStrategy;
import com.example.baseballorders.simulator.domain.player.strategy.bunt.BuntStrategy;
import com.example.baseballorders.simulator.domain.player.strategy.steal.StealStrategy;
import java.util.List;
import org.springframework.stereotype.Component;

/** Maps player data received through SQS to the simulation domain model. */
@Component
public class LineUpMapper {

    private final HittingStrategy hittingStrategy;
    private final StealStrategy stealStrategy;
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
            HittingStrategy hittingStrategy,
            StealStrategy stealStrategy,
            BuntStrategy buntStrategy) {
        this.hittingStrategy = hittingStrategy;
        this.stealStrategy = stealStrategy;
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
                                                battingBehaviorFor(player.personality()),
                                                player.stealEnabled()
                                                        ? stealStrategyFor(player.personality())
                                                        : noStealStrategy,
                                                player.buntEnabled()
                                                        ? buntStrategyFor(player.personality())
                                                        : noBuntStrategy))
                        .toList();
        return new LineUpEntity(batters);
    }

    private HittingStrategy battingBehaviorFor(PlayerPersonality personality) {
        return switch (personality) {
            case DEFAULT, EAGER_STEAL, EAGER_BUNT -> hittingStrategy;
            case EAGER_SLUGGISH -> BehaviorStrategies.longDistanceAtBat();
        };
    }

    private StealStrategy stealStrategyFor(PlayerPersonality personality) {
        return switch (personality) {
            case DEFAULT, EAGER_SLUGGISH, EAGER_BUNT -> stealStrategy;
            case EAGER_STEAL -> BehaviorStrategies.eagerSteal();
        };
    }

    private BuntStrategy buntStrategyFor(PlayerPersonality personality) {
        return switch (personality) {
            case DEFAULT, EAGER_SLUGGISH, EAGER_STEAL -> buntStrategy;
            case EAGER_BUNT -> BehaviorStrategies.eagerBunt();
        };
    }
}

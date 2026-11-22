package com.example.baseballorders.simulator.infrastructure.messaging;

import com.example.baseballorders.messaging.PlayerPersonality;
import com.example.baseballorders.messaging.SimulationPlayerMessage;
import com.example.baseballorders.simulator.domain.player.BatterEntity;
import com.example.baseballorders.simulator.domain.player.LineUpEntity;
import com.example.baseballorders.simulator.domain.player.strategy.BehaviorStrategies;
import com.example.baseballorders.simulator.domain.player.strategy.batting.HittingStrategy;
import com.example.baseballorders.simulator.domain.player.strategy.bunt.BuntStrategy;
import com.example.baseballorders.simulator.domain.player.strategy.steal.StealStrategy;
import com.example.baseballorders.simulator.infrastructure.config.SimulationPitcherProperties;
import java.util.List;
import org.springframework.stereotype.Component;

/** Maps player data received through gRPC to the simulation domain model. */
@Component
public class LineUpMapper {

    private final HittingStrategy hittingStrategy;
    private final StealStrategy stealStrategy;
    private final BuntStrategy buntStrategy;
    private final BehaviorStrategies behaviorStrategies;
    private final SimulationPitcherProperties pitcherProperties;
    private final StealStrategy noStealStrategy;
    private final BuntStrategy noBuntStrategy;
    private final HittingStrategy shortDistanceHittingStrategy;
    private final HittingStrategy highOnBaseHittingStrategy;

    /**
     * Creates a mapper using the default batting and stealing strategies.
     *
     * @param hittingStrategy middle-distance batting behavior assigned to middle-distance, eager
     *     steal, and eager bunt batters
     * @param stealStrategy stealing strategy assigned to each batter
     * @param buntStrategy bunt strategy assigned to each batter
     * @param behaviorStrategies 設定された確率を保持する行動戦略ファクトリ
     * @param pitcherProperties 設定から供給された既定の投手補正倍率
     */
    public LineUpMapper(
            HittingStrategy hittingStrategy,
            StealStrategy stealStrategy,
            BuntStrategy buntStrategy,
            BehaviorStrategies behaviorStrategies,
            SimulationPitcherProperties pitcherProperties) {
        this.hittingStrategy = hittingStrategy;
        this.stealStrategy = stealStrategy;
        this.buntStrategy = buntStrategy;
        this.behaviorStrategies = behaviorStrategies;
        this.pitcherProperties = pitcherProperties;
        noStealStrategy = behaviorStrategies.noSteal();
        noBuntStrategy = behaviorStrategies.noBunt();
        shortDistanceHittingStrategy = behaviorStrategies.shortDistanceHittingStrategy();
        highOnBaseHittingStrategy = behaviorStrategies.highOnBaseHittingStrategy();
    }

    /**
     * Converts gRPC player data to a domain lineup, disabling steals and bunts according to each
     * player's selection. The {@code DEFAULT} personality (single hitter) uses short-distance
     * batting, and the high on-base personality uses its dedicated walk probability. The legacy
     * {@code hit_average} wire field is interpreted as the domain batting average excluding walks
     * to retain the existing message contract. The opposing pitcher's personality carried by a
     * request is ignored; every batter receives the same standard probability adjustment. Bunt and
     * steal success rates are fixed simulation constants supplied through configuration rather than
     * per-player wire data.
     *
     * @param players players contained in a simulation request
     * @return lineup containing mapped batter entities in request order
     * @throws IllegalArgumentException when a player carries an unrecognized personality
     */
    public LineUpEntity map(List<SimulationPlayerMessage> players) {
        List<BatterEntity> batters =
                players.stream()
                        .map(
                                player ->
                                        new BatterEntity(
                                                player.getHitAverage() * battingAverageMultiplier(),
                                                battingBehaviorFor(player.getPersonality()),
                                                player.getStealEnabled()
                                                        ? stealStrategyFor(player.getPersonality())
                                                        : noStealStrategy,
                                                player.getBuntEnabled()
                                                        ? buntStrategyFor(player.getPersonality())
                                                        : noBuntStrategy))
                        .toList();
        return new LineUpEntity(batters);
    }

    private float battingAverageMultiplier() {
        return pitcherProperties.standard().onBaseMultiplier();
    }

    private HittingStrategy battingBehaviorFor(PlayerPersonality personality) {
        return switch (personality) {
            case PLAYER_PERSONALITY_DEFAULT -> shortDistanceHittingStrategy;
            case PLAYER_PERSONALITY_MIDDLE_DISTANCE,
                    PLAYER_PERSONALITY_EAGER_STEAL,
                    PLAYER_PERSONALITY_EAGER_BUNT ->
                    hittingStrategy;
            case PLAYER_PERSONALITY_EAGER_SLUGGISH -> behaviorStrategies.longDistanceAtBat();
            case PLAYER_PERSONALITY_HIGH_ON_BASE -> highOnBaseHittingStrategy;
            case UNRECOGNIZED -> throw unrecognized(personality);
        };
    }

    private StealStrategy stealStrategyFor(PlayerPersonality personality) {
        return switch (personality) {
            case PLAYER_PERSONALITY_DEFAULT,
                    PLAYER_PERSONALITY_MIDDLE_DISTANCE,
                    PLAYER_PERSONALITY_EAGER_SLUGGISH,
                    PLAYER_PERSONALITY_HIGH_ON_BASE,
                    PLAYER_PERSONALITY_EAGER_BUNT ->
                    stealStrategy;
            case PLAYER_PERSONALITY_EAGER_STEAL -> behaviorStrategies.eagerSteal();
            case UNRECOGNIZED -> throw unrecognized(personality);
        };
    }

    private BuntStrategy buntStrategyFor(PlayerPersonality personality) {
        return switch (personality) {
            case PLAYER_PERSONALITY_DEFAULT,
                    PLAYER_PERSONALITY_MIDDLE_DISTANCE,
                    PLAYER_PERSONALITY_EAGER_SLUGGISH,
                    PLAYER_PERSONALITY_HIGH_ON_BASE,
                    PLAYER_PERSONALITY_EAGER_STEAL ->
                    buntStrategy;
            case PLAYER_PERSONALITY_EAGER_BUNT -> behaviorStrategies.eagerBunt();
            case UNRECOGNIZED -> throw unrecognized(personality);
        };
    }

    private static IllegalArgumentException unrecognized(PlayerPersonality personality) {
        return new IllegalArgumentException("Unrecognized player personality: " + personality);
    }
}

package com.example.baseballorders.simulator.domain.player.strategy;

import com.example.baseballorders.simulator.domain.player.strategy.batting.HighOnBaseHittingStrategy;
import com.example.baseballorders.simulator.domain.player.strategy.batting.HittingStrategy;
import com.example.baseballorders.simulator.domain.player.strategy.batting.LongDistanceHittingStrategy;
import com.example.baseballorders.simulator.domain.player.strategy.batting.MiddleDistanceHittingStrategy;
import com.example.baseballorders.simulator.domain.player.strategy.batting.ShortDistanceHittingStrategy;
import com.example.baseballorders.simulator.domain.player.strategy.bunt.BuntStrategy;
import com.example.baseballorders.simulator.domain.player.strategy.bunt.EagerBuntStrategy;
import com.example.baseballorders.simulator.domain.player.strategy.bunt.NowayBuntStrategy;
import com.example.baseballorders.simulator.domain.player.strategy.bunt.StandardBuntStrategy;
import com.example.baseballorders.simulator.domain.player.strategy.steal.EagerStealStrategy;
import com.example.baseballorders.simulator.domain.player.strategy.steal.NowayStealStrategy;
import com.example.baseballorders.simulator.domain.player.strategy.steal.StandardStealStrategy;
import com.example.baseballorders.simulator.domain.player.strategy.steal.StealStrategy;
import com.example.baseballorders.simulator.domain.rule.BattingProbabilities;
import com.example.baseballorders.simulator.domain.rule.BattingProbabilitiesBuilder;
import com.example.baseballorders.simulator.domain.rule.SimulationRules;
import lombok.RequiredArgsConstructor;

/**
 * 設定された確率を保持し、具体実装を公開せずに行動戦略を生成する。
 *
 * <p>確率設定はコンストラクタで受け取る。生成メソッドは引数を取らない。
 */
@RequiredArgsConstructor
public final class BehaviorStrategies {

    private final SimulationRules rules;

    /**
     * Creates the middle-distance batting behavior.
     *
     * @return a middle-distance batting behavior
     */
    public HittingStrategy middleDistanceHittingStrategy() {
        return new MiddleDistanceHittingStrategy(rules.batting(), rules.middleDistanceHitting());
    }

    /**
     * 単打中心の短距離打者の打撃を生成する。
     *
     * @return 短距離打者の打撃
     */
    public HittingStrategy shortDistanceHittingStrategy() {
        return new ShortDistanceHittingStrategy(rules.batting());
    }

    /**
     * 打率は変えず、四球確率だけを高出塁率打者用の設定値にした打撃を生成する。安打の配分は中距離打者と同じ設定を使う。
     *
     * @return 高出塁率打者の打撃
     */
    public HittingStrategy highOnBaseHittingStrategy() {
        BattingProbabilities highOnBaseBatting =
                BattingProbabilitiesBuilder.battingProbabilities()
                        .walkProbability(rules.highOnBaseWalkProbability())
                        .strikeoutProbabilityWhenNotOnBase(
                                rules.batting().strikeoutProbabilityWhenNotOnBase())
                        .build();
        return new HighOnBaseHittingStrategy(highOnBaseBatting, rules.middleDistanceHitting());
    }

    /**
     * Creates the long-distance batting behavior.
     *
     * @return a long-distance batting behavior
     */
    public HittingStrategy longDistanceAtBat() {
        return new LongDistanceHittingStrategy(rules.batting(), rules.longDistanceHitting());
    }

    /**
     * Creates the eager stealing behavior.
     *
     * @return an eager stealing behavior
     */
    public StealStrategy eagerSteal() {
        return new EagerStealStrategy(rules.eagerSteal(), rules.stealSuccessRate());
    }

    /**
     * Creates the standard stealing behavior.
     *
     * @return a standard stealing behavior
     */
    public StealStrategy standardSteal() {
        return new StandardStealStrategy(rules.standardSteal(), rules.stealSuccessRate());
    }

    /**
     * Creates the stealing behavior that never attempts a steal.
     *
     * @return a no-steal behavior
     */
    public StealStrategy noSteal() {
        return new NowayStealStrategy();
    }

    /**
     * Creates the standard bunt strategy.
     *
     * @return a standard bunt strategy
     */
    public BuntStrategy standardBunt() {
        return new StandardBuntStrategy(rules.buntProbabilities());
    }

    /**
     * Creates the eager bunt strategy.
     *
     * @return an eager bunt strategy
     */
    public BuntStrategy eagerBunt() {
        return new EagerBuntStrategy(rules.buntProbabilities());
    }

    /**
     * Creates the bunt strategy that never attempts a bunt.
     *
     * @return a no-bunt strategy
     */
    public BuntStrategy noBunt() {
        return new NowayBuntStrategy();
    }
}

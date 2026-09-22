package com.example.baseballorders.simulator.domain.player;

import com.example.baseballorders.simulator.domain.play.BattingResult;
import com.example.baseballorders.simulator.domain.play.BuntResult;
import com.example.baseballorders.simulator.domain.play.BuntType;
import com.example.baseballorders.simulator.domain.play.OutCount;
import com.example.baseballorders.simulator.domain.play.StealResult;
import com.example.baseballorders.simulator.domain.play.StealTarget;
import com.example.baseballorders.simulator.domain.player.strategy.batting.HittingStrategy;
import com.example.baseballorders.simulator.domain.player.strategy.bunt.BuntStrategy;
import com.example.baseballorders.simulator.domain.player.strategy.steal.StealStrategy;
import com.example.baseballorders.simulator.domain.statistics.PlayResultObserver;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/** 打者 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BatterEntity extends Player {

    private static final PlayResultObserver NO_OPERATION_OBSERVER =
            new PlayResultObserver() {
                @Override
                public void onBattingResult(BattingResult battingResult, int runnerCount) {}

                @Override
                public void onBuntResult(BuntResult buntResult, BuntType buntType) {}

                @Override
                public void onStealResult(StealResult stealResult, StealTarget stealTarget) {}
            };

    /** 出塁率 */
    private final float onBasePercentage;

    /** 長打率 */
    private final float sluggish;

    /** バント成功率 */
    private final float buntSuccessRate;

    /** 盗塁成功率 */
    private final float stealSuccessRate;

    /** 打撃戦略 */
    private final HittingStrategy hittingStrategy;

    /** 走塁戦略 */
    private final StealStrategy stealStrategy;

    /** バント戦略 */
    private final BuntStrategy buntStrategy;

    /** プレー結果の通知先 */
    private final PlayResultObserver playResultObserver;

    /**
     * Creates a batter without a play-result observer.
     *
     * @param onBasePercentage on-base percentage
     * @param sluggish slugging percentage
     * @param buntSuccessRate bunt success rate
     * @param stealSuccessRate steal success rate
     * @param hittingStrategy batting behavior
     * @param stealStrategy steal strategy
     * @param buntStrategy bunt strategy
     */
    public BatterEntity(
            float onBasePercentage,
            float sluggish,
            float buntSuccessRate,
            float stealSuccessRate,
            HittingStrategy hittingStrategy,
            StealStrategy stealStrategy,
            BuntStrategy buntStrategy) {
        this(
                onBasePercentage,
                sluggish,
                buntSuccessRate,
                stealSuccessRate,
                hittingStrategy,
                stealStrategy,
                buntStrategy,
                NO_OPERATION_OBSERVER);
    }

    /**
     * 打撃戦略に従って打撃する。
     *
     * @param runnerCount 打撃前の走者数
     * @return 打席結果。結果を購読者へ通知する
     */
    public BattingResult swing(int runnerCount) {
        BattingResult battingResult = hittingStrategy.batting(this.onBasePercentage, this.sluggish);
        playResultObserver.onBattingResult(battingResult, runnerCount);
        return battingResult;
    }

    /**
     * 二塁への盗塁を試みる。
     *
     * @return 盗塁結果。結果を購読者へ通知する
     */
    public StealResult stealToDouble() {
        StealResult stealResult = stealStrategy.runToDouble(stealSuccessRate);
        playResultObserver.onStealResult(stealResult, StealTarget.SECOND);
        return stealResult;
    }

    /**
     * 三塁への盗塁を試みる。
     *
     * @return 盗塁結果。結果を購読者へ通知する
     */
    public StealResult stealToTriple() {
        StealResult stealResult = stealStrategy.runToTriple(stealSuccessRate);
        playResultObserver.onStealResult(stealResult, StealTarget.THIRD);
        return stealResult;
    }

    /**
     * アウト数と塁状態を考慮し、バント戦略に従ってバントする。
     *
     * @param outCount 現在のアウト数
     * @param buntType 現在の走者配置に対応するバント種別
     * @return バント結果。結果を購読者へ通知する
     */
    public BuntResult bunt(OutCount outCount, BuntType buntType) {
        BuntResult buntResult = buntStrategy.bunt(buntSuccessRate, outCount);
        playResultObserver.onBuntResult(buntResult, buntType);
        return buntResult;
    }

    /**
     * Creates a copy that notifies the supplied observer of each play result.
     *
     * @param observer observer for a single game's play results
     * @return batter copy bound to the observer
     */
    public BatterEntity observedBy(PlayResultObserver observer) {
        return new BatterEntity(
                onBasePercentage,
                sluggish,
                buntSuccessRate,
                stealSuccessRate,
                hittingStrategy,
                stealStrategy,
                buntStrategy,
                Objects.requireNonNull(observer));
    }
}

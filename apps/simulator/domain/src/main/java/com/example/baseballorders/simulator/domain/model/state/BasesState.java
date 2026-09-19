package com.example.baseballorders.simulator.domain.model.state;

import com.example.baseballorders.simulator.domain.code.Base;
import com.example.baseballorders.simulator.domain.model.player.BatterEntity;
import java.util.Optional;

/** 走者配置と、その配置における打撃結果の適用規則を表す不変の塁状態。 */
public abstract class BasesState {
    private final BatterEntity firstRunner;
    private final BatterEntity secondRunner;
    private final BatterEntity thirdRunner;

    protected BasesState(
            BatterEntity firstRunner, BatterEntity secondRunner, BatterEntity thirdRunner) {
        this.firstRunner = firstRunner;
        this.secondRunner = secondRunner;
        this.thirdRunner = thirdRunner;
    }

    protected final BatterEntity runnerAt(Base base) {
        return switch (base) {
            case FIRST -> firstRunner;
            case SECOND -> secondRunner;
            case THIRD -> thirdRunner;
        };
    }

    /**
     * 現在の走者数を返す。
     *
     * @return 現在塁上にいる走者の数
     */
    public final int runnerCount() {
        return (firstRunner == null ? 0 : 1)
                + (secondRunner == null ? 0 : 1)
                + (thirdRunner == null ? 0 : 1);
    }

    /**
     * 指定した塁に走者がいるかを返す。
     *
     * @param base 確認する塁
     * @return 指定した塁に走者がいれば {@code true}
     */
    public final boolean isOccupied(Base base) {
        return runnerAt(base) != null;
    }

    /**
     * 現在の塁配置で実行可能な盗塁を返す。
     *
     * @return 走者が次の塁へ盗塁できる場合はその候補、できない場合は空
     */
    public final Optional<Stealable> stealOpportunity() {
        return this instanceof Stealable stealable && stealable.runner() != null
                ? Optional.of(stealable)
                : Optional.empty();
    }

    /**
     * 現在の塁配置で実行可能な犠打を返す。
     *
     * @return 走者を進める犠打を試みられる場合はその候補、できない場合は空
     */
    public final Optional<Buntable> buntOpportunity() {
        return this instanceof Buntable buntable ? Optional.of(buntable) : Optional.empty();
    }

    /**
     * 単打を適用した結果を返す。
     *
     * @param batter 単打を打った打者
     * @return 次の塁状態とこのプレーで入る得点
     */
    public final BaseTransition hitSingle(BatterEntity batter) {
        return new BaseTransition(
                advance(Base.FIRST).withRunnerAt(Base.FIRST, batter),
                isOccupied(Base.THIRD) ? 1 : 0);
    }

    /**
     * 二塁打を適用した結果を返す。
     *
     * @param batter 二塁打を打った打者
     * @return 次の塁状態とこのプレーで入る得点
     */
    public final BaseTransition hitDouble(BatterEntity batter) {
        return new BaseTransition(
                advance(Base.SECOND).withRunnerAt(Base.SECOND, batter),
                (isOccupied(Base.SECOND) ? 1 : 0) + (isOccupied(Base.THIRD) ? 1 : 0));
    }

    /**
     * 三塁打を適用した結果を返す。
     *
     * @param batter 三塁打を打った打者
     * @return 次の塁状態とこのプレーで入る得点
     */
    public final BaseTransition hitTriple(BatterEntity batter) {
        return new BaseTransition(
                advance(Base.THIRD).withRunnerAt(Base.THIRD, batter), runnerCount());
    }

    /**
     * 本塁打を適用した結果を返す。
     *
     * @return 走者を一掃した塁状態とこのプレーで入る得点
     */
    public final BaseTransition hitHomer() {
        return new BaseTransition(empty(), runnerCount() + 1L);
    }

    /**
     * 盗塁成功を適用した結果を返す。
     *
     * @param stealable 盗塁を成功させた走者と進塁経路
     * @return 次の塁状態とこのプレーで入る得点
     */
    public final BaseTransition succeedSteal(Stealable stealable) {
        return new BaseTransition(moveRunner(stealable.sourceBase(), stealable.targetBase()), 0);
    }

    /**
     * 盗塁死を適用した結果を返す。
     *
     * @param stealable 盗塁死となった走者と出発塁
     * @return 次の塁状態とこのプレーで入る得点
     */
    public final BaseTransition caughtStealing(Stealable stealable) {
        return new BaseTransition(withRunnerAt(stealable.sourceBase(), null), 0);
    }

    /**
     * 成功した犠打を適用した結果を返す。
     *
     * @return 次の塁状態とこのプレーで入る得点
     */
    public final BaseTransition sacrificeBunt() {
        return new BaseTransition(advance(Base.FIRST), 0);
    }

    private BasesState advance(Base bases) {
        return switch (bases) {
            case FIRST -> of(null, firstRunner, secondRunner);
            case SECOND -> of(null, null, firstRunner);
            case THIRD -> empty();
        };
    }

    private BasesState moveRunner(Base source, Base destination) {
        return withRunnerAt(destination, runnerAt(source)).withRunnerAt(source, null);
    }

    private BasesState withRunnerAt(Base base, BatterEntity runner) {
        return switch (base) {
            case FIRST -> of(runner, secondRunner, thirdRunner);
            case SECOND -> of(firstRunner, runner, thirdRunner);
            case THIRD -> of(firstRunner, secondRunner, runner);
        };
    }

    /**
     * 走者なしの塁状態を返す。
     *
     * @return 走者がいない塁状態
     */
    public static BasesState empty() {
        return new NoBasesState();
    }

    private static BasesState of(BatterEntity first, BatterEntity second, BatterEntity third) {
        if (first != null && second != null && third != null)
            return new FullBasesState(first, second, third);
        if (first != null && second != null) return new FirstDoubleBaseState(first, second);
        if (first != null && third != null) return new FirstThirdBaseState(first, third);
        if (first != null) return new SingleBasesState(first);
        if (second != null && third != null) return new DoubleThirdBaseState(second, third);
        if (second != null) return new DoubleBaseState(second);
        if (third != null) return new ThirdBaseState(third);
        return empty();
    }
}

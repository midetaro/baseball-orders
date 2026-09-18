package com.example.baseballorders.simulator.domain.model.state;

import com.example.baseballorders.simulator.domain.code.Base;
import com.example.baseballorders.simulator.domain.model.GameBattingContext;
import com.example.baseballorders.simulator.domain.model.player.BatterEntity;

/** 走者配置と、その配置における打撃結果の適用規則を表す不変の塁状態。 */
public abstract class BasesState {
    private final BatterEntity firstRunner;
    private final BatterEntity secondRunner;
    private final BatterEntity thirdRunner;
    protected BasesState(BatterEntity firstRunner, BatterEntity secondRunner, BatterEntity thirdRunner) {
        this.firstRunner = firstRunner; this.secondRunner = secondRunner; this.thirdRunner = thirdRunner;
    }
    /** 指定した塁の走者を返す。走者がいない場合は {@code null} を返す。 */
    public final BatterEntity runnerAt(Base base) { return switch (base) { case FIRST -> firstRunner; case SECOND -> secondRunner; case THIRD -> thirdRunner; }; }
    /** 現在の走者数を返す。 */
    public final int runnerCount() { return (firstRunner == null ? 0 : 1) + (secondRunner == null ? 0 : 1) + (thirdRunner == null ? 0 : 1); }
    /** 全走者を指定した塁数だけ進めた次の塁状態を返す。 */
    public final BasesState advance(Base bases) { return switch (bases) { case FIRST -> of(null, firstRunner, secondRunner); case SECOND -> of(null, null, firstRunner); case THIRD -> empty(); }; }
    /** 指定した塁の走者を置換した次の塁状態を返す。 */
    public final BasesState withRunnerAt(Base base, BatterEntity runner) { return switch (base) { case FIRST -> of(runner, secondRunner, thirdRunner); case SECOND -> of(firstRunner, runner, thirdRunner); case THIRD -> of(firstRunner, secondRunner, runner); }; }
    /** アウトとなった打撃結果を適用する。 */
    public void out(GameBattingContext context) { context.addOutCounts(1); }
    /** 単打を適用した次の塁状態を返す。 */
    public abstract BasesState hitSingle(GameBattingContext context, BatterEntity batterEntity);
    /** 二塁打を適用した次の塁状態を返す。 */
    public abstract BasesState hitDouble(GameBattingContext context, BatterEntity batterEntity);
    /** 三塁打を適用した次の塁状態を返す。 */
    public abstract BasesState hitTriple(GameBattingContext context, BatterEntity batterEntity);
    /** 本塁打を適用した次の塁状態を返す。 */
    public abstract BasesState hitHomer(GameBattingContext context, BatterEntity batterEntity);
    /** 走者なしの塁状態を返す。 */
    public static BasesState empty() { return new NoBasesState(); }
    private static BasesState of(BatterEntity first, BatterEntity second, BatterEntity third) {
        if (first != null && second != null && third != null) return new FullBasesState(first, second, third);
        if (first != null && second != null) return new FirstDoubleBaseState(first, second);
        if (first != null && third != null) return new FirstThirdBaseState(first, third);
        if (first != null) return new SingleBasesState(first);
        if (second != null && third != null) return new DoubleThirdBaseState(second, third);
        if (second != null) return new DoubleBaseState(second);
        if (third != null) return new ThirdBaseState(third);
        return empty();
    }
}

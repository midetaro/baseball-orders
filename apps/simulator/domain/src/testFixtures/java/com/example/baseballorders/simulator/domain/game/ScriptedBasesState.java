package com.example.baseballorders.simulator.domain.game;

import com.example.baseballorders.simulator.domain.play.OutCount;
import com.example.baseballorders.simulator.domain.player.BatterEntity;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * イニングの進み方を脚本どおりに適用する {@link BasesState} のフェイク。
 *
 * <p>どのプレーが来ても「脚本の次の打席」を 1 つ消化する。イニングの先頭打席で得点を加算し、 脚本の打席数に達したらイニングを完了させる。走者は常に不在として扱うので、盗塁・バントの
 * 能力を一切持たず、1 打席あたりの乱数消費は打撃の 1 個だけになる。
 *
 * <p>走者配置ごとの進塁・得点規則は担保しない。それは L2 の各 {@code BasesState} テストの担当である。
 */
final class ScriptedBasesState implements BasesState {

    private final InningStateContext context;
    private final Deque<InningPlan> plans;
    private InningPlan currentPlan;
    private int atBatsApplied;

    ScriptedBasesState(InningStateContext context, InningScript script) {
        this.context = context;
        plans = new ArrayDeque<>(script.plans());
    }

    @Override
    public OutCount getOutCount() {
        return context.outCount();
    }

    @Override
    public BatterEntity runnerAt(Base base) {
        return null;
    }

    @Override
    public int runnerCount() {
        return 0;
    }

    @Override
    public void out() {
        applyScriptedAtBat();
    }

    @Override
    public void battingOut() {
        applyScriptedAtBat();
    }

    @Override
    public void walk(BatterEntity batter) {
        applyScriptedAtBat();
    }

    @Override
    public void hitSingle(BatterEntity batter) {
        applyScriptedAtBat();
    }

    @Override
    public void hitDouble(BatterEntity batter) {
        applyScriptedAtBat();
    }

    @Override
    public void hitTriple(BatterEntity batter) {
        applyScriptedAtBat();
    }

    @Override
    public void hitHomer() {
        applyScriptedAtBat();
    }

    /** 脚本を最後まで消化したことを検証する。 */
    void assertFullyPlayed() {
        if (currentPlan != null || !plans.isEmpty()) {
            throw new AssertionError(
                    "イニング脚本が余った: 未消化のイニングが"
                            + (plans.size() + (currentPlan == null ? 0 : 1))
                            + "回ぶん残っている");
        }
    }

    private void applyScriptedAtBat() {
        if (context.isGameOver()) {
            return;
        }
        if (currentPlan == null) {
            currentPlan = plans.pollFirst();
            if (currentPlan == null) {
                throw new AssertionError("イニング脚本が尽きた: " + context.inning() + "回の打席が要求された");
            }
            if (currentPlan.inning() != context.inning()) {
                throw new AssertionError(
                        "イニング脚本と試合の回数がずれている: 脚本="
                                + currentPlan.inning()
                                + "回, 試合="
                                + context.inning()
                                + "回");
            }
            context.addScore(currentPlan.runs());
            atBatsApplied = 0;
        }
        context.addOut();
        atBatsApplied++;
        if (atBatsApplied >= currentPlan.outs()) {
            context.reset();
            context.completeInning();
            currentPlan = null;
        }
    }
}

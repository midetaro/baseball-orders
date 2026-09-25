package com.example.baseballorders.simulator.domain.game;

import java.util.List;
import java.util.stream.IntStream;

/**
 * 1 試合ぶんのイニング脚本。
 *
 * <p>9 イニングを乱数だけで駆動すると最低 27 アウトぶん・数十個の乱数列が必要になり、シナリオが読めなくなる。 イニングの進み方そのものを脚本にすることで、試合レベルの関心事（9
 * イニングの進行・得点の合算・完了通知・統計の集計）だけを残す。
 */
public record InningScript(List<InningPlan> plans) {

    /** 1 試合のイニング数。 */
    public static final int INNINGS_PER_GAME = 9;

    /**
     * イニング脚本を並べて 1 試合の脚本を作成する。
     *
     * @param plans 1 回から順に並べたイニング脚本
     * @return 試合の脚本
     */
    public static InningScript of(InningPlan... plans) {
        return new InningScript(List.of(plans));
    }

    /**
     * 各イニングの得点だけを与え、すべて 3 アウトで終わる 9 イニングの脚本を作成する。
     *
     * @param runsPerInning 1 回から 9 回までの得点
     * @return 試合の脚本
     * @throws IllegalArgumentException 得点が 9 個でない場合
     */
    public static InningScript ofRunsPerInning(long... runsPerInning) {
        if (runsPerInning.length != INNINGS_PER_GAME) {
            throw new IllegalArgumentException("得点は9イニングぶん必要である: " + runsPerInning.length);
        }
        return new InningScript(
                IntStream.range(0, INNINGS_PER_GAME)
                        .mapToObj(index -> InningPlan.scoring(index + 1, runsPerInning[index]))
                        .toList());
    }

    /**
     * 回数の連番を検証したうえで試合の脚本を作成する。
     *
     * @param plans 1 回から順に並べたイニング脚本
     * @throws IllegalArgumentException イニング脚本が 1 個以上 9 個以下でない場合、または回数が 1 から連番になっていない場合
     */
    public InningScript {
        if (plans.isEmpty() || plans.size() > INNINGS_PER_GAME) {
            throw new IllegalArgumentException("イニング脚本は1個以上9個以下でなければならない: " + plans.size());
        }
        for (int index = 0; index < plans.size(); index++) {
            int expectedInning = index + 1;
            if (plans.get(index).inning() != expectedInning) {
                throw new IllegalArgumentException(
                        "イニング脚本は1回から連番でなければならない: "
                                + expectedInning
                                + "回目が"
                                + plans.get(index).inning()
                                + "回になっている");
            }
        }
        plans = List.copyOf(plans);
    }

    /**
     * この脚本を消化するのに必要な打席数を返す。
     *
     * @return 全イニングの打席数の合計
     */
    public int totalAtBats() {
        int totalAtBats = 0;
        for (InningPlan plan : plans) {
            totalAtBats += plan.outs();
        }
        return totalAtBats;
    }

    /**
     * この脚本の総得点を返す。
     *
     * @return 全イニングの得点の合計
     */
    public long totalRuns() {
        long totalRuns = 0;
        for (InningPlan plan : plans) {
            totalRuns += plan.runs();
        }
        return totalRuns;
    }
}

package com.example.baseballorders.simulator.domain.statistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 1試合実行で発生した状況推移を、発生順に蓄積する。 */
public final class GameTransitionRecorder {

    private final List<GameTransition> transitions = new ArrayList<>();

    /**
     * 発生した状況推移を記録の末尾へ追加する。
     *
     * @param transition 記録する状況推移
     */
    public void record(GameTransition transition) {
        transitions.add(transition);
    }

    /**
     * これまでに記録した状況推移を、発生順に並んだ変更不可のリストとして返す。
     *
     * @return 記録済みの状況推移一覧
     */
    public List<GameTransition> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(transitions));
    }
}

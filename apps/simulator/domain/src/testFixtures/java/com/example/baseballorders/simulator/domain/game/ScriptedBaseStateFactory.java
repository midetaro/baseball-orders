package com.example.baseballorders.simulator.domain.game;

import com.example.baseballorders.simulator.domain.rule.SimulationRulesTestData;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * イニングの進み方を脚本化した {@link BasesState} を供給するファクトリ。
 *
 * <p>{@code GameBattingContext} と {@code SimulateGameUseCase} はどちらも {@link BaseStateFactory}
 * を受け取る公開 コンストラクタを持つので、本番コードを変更せずに差し替えられる。
 *
 * <p>1 試合ごとに 1 つの {@link InningScript} を消費する。{@code SimulateGameUseCase} は複数試合で同じファクトリを 使い回すため、試合（=
 * {@link InningStateContext}）ごとに脚本を割り当てる。
 */
public final class ScriptedBaseStateFactory extends BaseStateFactory {

    private final Deque<InningScript> gameScripts;
    private final Map<InningStateContext, ScriptedBasesState> statesByGame =
            new IdentityHashMap<>();
    private final List<ScriptedBasesState> startedGames = new ArrayList<>();

    private ScriptedBaseStateFactory(List<InningScript> gameScripts) {
        super(SimulationRulesTestData.standard().runnerAdvance());
        this.gameScripts = new ArrayDeque<>(gameScripts);
    }

    /**
     * 1 試合ぶんの脚本を持つファクトリを作成する。
     *
     * @param script 試合の脚本
     * @return 脚本化されたファクトリ
     */
    public static ScriptedBaseStateFactory of(InningScript script) {
        return new ScriptedBaseStateFactory(List.of(script));
    }

    /**
     * 試合ごとの脚本を順に消費するファクトリを作成する。
     *
     * @param scripts 1 試合目から順に並べた試合の脚本
     * @return 脚本化されたファクトリ
     */
    public static ScriptedBaseStateFactory ofGames(InningScript... scripts) {
        return new ScriptedBaseStateFactory(List.of(scripts));
    }

    @Override
    public BasesState create(InningStateContext context, int configuration) {
        return statesByGame.computeIfAbsent(context, this::startGame);
    }

    /**
     * 脚本を割り当てた試合の数を返す。
     *
     * @return 開始された試合数
     */
    public int startedGameCount() {
        return startedGames.size();
    }

    /**
     * すべての試合の脚本を最後まで消化したことを検証する。
     *
     * @throws AssertionError 未使用の試合脚本が残っている場合、または消化しきれていないイニングがある場合
     */
    public void assertFullyPlayed() {
        if (!gameScripts.isEmpty()) {
            throw new AssertionError("試合脚本が余った: 未使用の試合が" + gameScripts.size() + "件残っている");
        }
        startedGames.forEach(ScriptedBasesState::assertFullyPlayed);
    }

    private ScriptedBasesState startGame(InningStateContext context) {
        InningScript script = gameScripts.pollFirst();
        if (script == null) {
            throw new AssertionError("試合脚本が尽きた: " + (startedGames.size() + 1) + "試合目が要求された");
        }
        ScriptedBasesState state = new ScriptedBasesState(context, script);
        startedGames.add(state);
        return state;
    }
}

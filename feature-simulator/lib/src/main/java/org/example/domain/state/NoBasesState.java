package org.example.domain.state;

import org.example.domain.code.BattingResult;
import org.example.domain.game.GameContext;
import org.example.domain.player.Batter;

public class NoBasesState implements BasesState {
    @Override
    public void handle(GameContext context, Batter batter) {
        // 走者なしの状態での処理
        System.out.println("走者なしの状態で打席を処理します。");
        // ここで打撃結果に応じた処理を行う
        var result = batter.swing();
        if (result == BattingResult.OUT) {
            context.addScore(0);
            context.addOut(1);
        } else if (result == BattingResult.HIT_SINGLE) {
            context.addScore(0);
        } else if (result == BattingResult.HIT_DOUBLE) {
            context.addScore(0);
        } else if (result == BattingResult.HIT_TRIPLE) {
            context.addScore(1);
        } else if (result == BattingResult.HIT_HOMER) {
            context.addScore(1);
        }
    }
}

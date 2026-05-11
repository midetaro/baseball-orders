package org.example.domain.state;

import org.example.domain.game.GameContext;
import org.example.domain.player.Batter;

public class FullBasesState implements BasesState {

    @Override
    public void handle(GameContext context, Batter batter) {
        // 走者満塁の状態での処理
        System.out.println("走者満塁の状態で打席を処理します。");
        // ここで打撃結果に応じた処理を行う
    }
}

package org.example.domain.state;

import org.example.domain.game.GameContext;
import org.example.domain.player.Batter;

public interface BasesState {

    void handle(GameContext context, Batter batter);

}

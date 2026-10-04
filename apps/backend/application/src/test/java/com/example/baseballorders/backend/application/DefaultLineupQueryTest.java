package com.example.baseballorders.backend.application;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.baseballorders.backend.domain.DefaultBatter;
import com.example.baseballorders.backend.domain.DefaultBatterBuilder;
import com.example.baseballorders.backend.domain.PlayerPersonality;
import com.example.baseballorders.backend.domain.TeamDefaultLineup;
import com.example.baseballorders.backend.domain.TeamDefaultLineupBuilder;
import com.example.baseballorders.backend.domain.TeamStrength;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DefaultLineupQueryTest {

    private static TeamDefaultLineup lineup(TeamStrength team) {
        List<DefaultBatter> batters = new ArrayList<>();
        for (int order = 1; order <= 9; order++) {
            batters.add(
                    DefaultBatterBuilder.defaultBatter()
                            .battingOrder(order)
                            .hitAverage(0.25f)
                            .personality(PlayerPersonality.DEFAULT)
                            .stealForced(false)
                            .buntForced(false)
                            .build());
        }
        return TeamDefaultLineupBuilder.teamDefaultLineup().team(team).batters(batters).build();
    }

    @Test
    @DisplayName("リポジトリのチーム一覧を順序を保って返す")
    void returnsRepositoryTeamsInOrder() {
        // given
        // リポジトリの返却値を固定するスタブ。
        List<TeamDefaultLineup> stored =
                List.of(
                        lineup(TeamStrength.STRONG),
                        lineup(TeamStrength.AVERAGE),
                        lineup(TeamStrength.WEAK));
        DefaultLineupQuery sut = new DefaultLineupQuery(() -> stored);

        // when
        List<TeamDefaultLineup> teams = sut.findAllTeams();

        // then
        assertAll(
                () -> assertEquals(stored, teams),
                () ->
                        assertThrows(
                                UnsupportedOperationException.class,
                                () -> teams.add(lineup(TeamStrength.WEAK))));
    }
}

package com.example.baseballorders.backend.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TeamDefaultLineupTest {

    private static List<DefaultBatter> batters(int count) {
        List<DefaultBatter> batters = new ArrayList<>();
        for (int order = 1; order <= count; order++) {
            batters.add(
                    DefaultBatterBuilder.defaultBatter()
                            .battingOrder(order)
                            .hitAverage(0.25f)
                            .personality(PlayerPersonality.DEFAULT)
                            .stealForced(false)
                            .buntForced(false)
                            .build());
        }
        return batters;
    }

    private static TeamDefaultLineup lineup(TeamStrength team, List<DefaultBatter> batters) {
        return TeamDefaultLineupBuilder.teamDefaultLineup().team(team).batters(batters).build();
    }

    @Test
    @DisplayName("9人の打者から既定オーダーを生成し、打者リストを防御的にコピーする")
    void createsLineupWithDefensiveCopy() {
        // given
        List<DefaultBatter> source = batters(9);

        // when
        TeamDefaultLineup lineup = lineup(TeamStrength.STRONG, source);
        source.clear();

        // then
        assertAll(
                () -> assertEquals(TeamStrength.STRONG, lineup.team()),
                () -> assertEquals(9, lineup.batters().size()),
                () ->
                        assertThrows(
                                UnsupportedOperationException.class,
                                () -> lineup.batters().clear()));
    }

    @Test
    @DisplayName("打者が9人でなければ拒否する")
    void rejectsWrongBatterCount() {
        // given
        // when
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> lineup(TeamStrength.WEAK, batters(8)));

        // then
        assertAll(
                () ->
                        assertEquals(
                                "batters must contain exactly 9 batters", exception.getMessage()));
    }

    @Test
    @DisplayName("打者が打順どおりに並んでいなければ拒否する")
    void rejectsUnorderedBatters() {
        // given
        List<DefaultBatter> reversed = new ArrayList<>(batters(9));
        java.util.Collections.reverse(reversed);

        // when
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> lineup(TeamStrength.AVERAGE, reversed));

        // then
        assertAll(() -> assertEquals("batters must be in batting order", exception.getMessage()));
    }

    @Test
    @DisplayName("チームがnullなら拒否する")
    void rejectsNullTeam() {
        // given
        // when
        NullPointerException exception =
                assertThrows(NullPointerException.class, () -> lineup(null, batters(9)));

        // then
        assertAll(() -> assertEquals("team must not be null", exception.getMessage()));
    }
}

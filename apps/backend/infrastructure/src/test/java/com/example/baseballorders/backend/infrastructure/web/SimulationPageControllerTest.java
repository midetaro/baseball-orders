package com.example.baseballorders.backend.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.baseballorders.backend.application.DefaultLineupQuery;
import com.example.baseballorders.backend.domain.DefaultBatter;
import com.example.baseballorders.backend.domain.DefaultBatterBuilder;
import com.example.baseballorders.backend.domain.PlayerPersonality;
import com.example.baseballorders.backend.domain.TeamDefaultLineup;
import com.example.baseballorders.backend.domain.TeamDefaultLineupBuilder;
import com.example.baseballorders.backend.domain.TeamStrength;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SimulationPageControllerTest {

    private static SimulationPageController controller() {
        // DBの代わりに強・並・弱の既定オーダーを返すリポジトリ
        var query =
                new DefaultLineupQuery(
                        () ->
                                List.of(
                                        lineup(TeamStrength.STRONG, 0.3f, true, false),
                                        lineup(TeamStrength.AVERAGE, 0.25f, false, true),
                                        lineup(TeamStrength.WEAK, 0.2f, false, false)));
        return new SimulationPageController(1000L, 1800L, 2200L, 3600L, 1600L, query);
    }

    private static DefaultBatter batter(
            int order, float hitAverage, boolean stealForced, boolean buntForced) {
        return DefaultBatterBuilder.defaultBatter()
                .battingOrder(order)
                .hitAverage(hitAverage)
                .personality(PlayerPersonality.MIDDLE_DISTANCE)
                .stealForced(stealForced)
                .buntForced(buntForced)
                .build();
    }

    private static TeamDefaultLineup lineup(
            TeamStrength team, float hitAverage, boolean stealForced, boolean buntForced) {
        return TeamDefaultLineupBuilder.teamDefaultLineup()
                .team(team)
                .batters(
                        IntStream.rangeClosed(1, 9)
                                .mapToObj(
                                        order -> batter(order, hitAverage, stealForced, buntForced))
                                .toList())
                .build();
    }

    @Test
    @DisplayName("1試合実行画面を通常・安打・得点・本塁打・バント成功のフレーム表示時間とともに返す")
    void showsSingleGamePage() {
        // given
        var sut = controller();

        // when
        var page = sut.singleGame();

        // then
        assertAll(
                () -> assertEquals("single-game", page.getViewName()),
                () -> assertEquals(1000L, page.getModel().get("frameDurationMillis")),
                () -> assertEquals(1800L, page.getModel().get("hitFrameDurationMillis")),
                () -> assertEquals(2200L, page.getModel().get("scoreFrameDurationMillis")),
                () -> assertEquals(3600L, page.getModel().get("homeRunFrameDurationMillis")),
                () -> assertEquals(1600L, page.getModel().get("buntFrameDurationMillis")));
    }

    @Test
    @DisplayName("トップ画面は打順組み替え画面を返す")
    void showsBattingOrderAtRoot() {
        // given
        var sut = controller();
        // when
        var page = sut.index();
        @SuppressWarnings("unchecked")
        var teams = (List<TeamLineupView>) page.getModel().get("teams");
        var strongBatter = teams.get(0).batters().get(0);
        // then
        assertAll(
                () -> assertEquals("batting-order", page.getViewName()),
                () -> assertEquals(1, page.getModel().size()),
                () ->
                        assertEquals(
                                List.of("STRONG", "AVERAGE", "WEAK"),
                                teams.stream().map(TeamLineupView::key).toList()),
                () ->
                        assertEquals(
                                List.of("強", "並", "弱"),
                                teams.stream().map(TeamLineupView::label).toList()),
                () -> assertEquals("0.300", strongBatter.hitAverage()),
                () -> assertEquals("MIDDLE_DISTANCE", strongBatter.personality()),
                () -> assertEquals(1, strongBatter.battingOrder()),
                () -> assertEquals(true, strongBatter.stealForced()),
                () -> assertEquals(false, strongBatter.buntForced()),
                () -> assertEquals(true, teams.get(1).batters().get(0).buntForced()));
    }

    @Test
    @DisplayName("大規模実行画面を表示すると既存の入力画面を返す")
    void showsLargeScaleSimulationPage() {
        // given
        var controller = controller();

        // when
        var page = controller.largeScale();

        // then
        assertAll(
                () -> assertEquals("simulation", page.getViewName()),
                () -> assertEquals(true, page.getModel().isEmpty()));
    }
}

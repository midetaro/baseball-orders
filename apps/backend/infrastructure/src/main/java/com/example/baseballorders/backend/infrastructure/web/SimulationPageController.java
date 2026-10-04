package com.example.baseballorders.backend.infrastructure.web;

import com.example.baseballorders.backend.application.DefaultLineupQuery;
import com.example.baseballorders.backend.domain.DefaultBatter;
import com.example.baseballorders.backend.domain.TeamDefaultLineup;
import com.example.baseballorders.backend.domain.TeamStrength;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

/** 画面入力で打順設定を行うシミュレーション画面を提供する。 */
@Controller
public final class SimulationPageController {

    private final long frameDurationMillis;
    private final long hitFrameDurationMillis;
    private final long scoreFrameDurationMillis;
    private final long homeRunFrameDurationMillis;
    private final long buntFrameDurationMillis;
    private final DefaultLineupQuery defaultLineupQuery;

    /**
     * 1試合実行結果のアニメーション表示に使う、演出の種類ごとのフレーム表示時間を受け取って構成を作成する。
     *
     * @param frameDurationMillis 演出のない通常フレームの表示時間（ミリ秒）
     * @param hitFrameDurationMillis 得点のない安打を演出するフレームの表示時間（ミリ秒）
     * @param scoreFrameDurationMillis 本塁打以外の得点を演出するフレームの表示時間（ミリ秒）
     * @param homeRunFrameDurationMillis 本塁打を演出するフレームの表示時間（ミリ秒）
     * @param buntFrameDurationMillis 得点のない進塁バント成功を演出するフレームの表示時間（ミリ秒）
     * @param defaultLineupQuery 打順組み替え画面に表示するチーム別既定オーダーの取得ユースケース
     */
    public SimulationPageController(
            @Value("${baseball-orders.rendering.single-game.frame-duration-millis}")
                    long frameDurationMillis,
            @Value("${baseball-orders.rendering.single-game.hit-frame-duration-millis}")
                    long hitFrameDurationMillis,
            @Value("${baseball-orders.rendering.single-game.score-frame-duration-millis}")
                    long scoreFrameDurationMillis,
            @Value("${baseball-orders.rendering.single-game.home-run-frame-duration-millis}")
                    long homeRunFrameDurationMillis,
            @Value("${baseball-orders.rendering.single-game.bunt-frame-duration-millis}")
                    long buntFrameDurationMillis,
            DefaultLineupQuery defaultLineupQuery) {
        this.frameDurationMillis = frameDurationMillis;
        this.hitFrameDurationMillis = hitFrameDurationMillis;
        this.scoreFrameDurationMillis = scoreFrameDurationMillis;
        this.homeRunFrameDurationMillis = homeRunFrameDurationMillis;
        this.buntFrameDurationMillis = buntFrameDurationMillis;
        this.defaultLineupQuery = defaultLineupQuery;
    }

    /**
     * 空の打順入力欄を含む1試合実行画面を、通常・安打・得点・本塁打・バント成功の各フレーム表示時間とともに表示する。
     *
     * @return 1試合実行画面
     */
    @GetMapping("/single-game")
    public ModelAndView singleGame() {
        var modelAndView = new ModelAndView("single-game");
        modelAndView.addObject("frameDurationMillis", frameDurationMillis);
        modelAndView.addObject("hitFrameDurationMillis", hitFrameDurationMillis);
        modelAndView.addObject("scoreFrameDurationMillis", scoreFrameDurationMillis);
        modelAndView.addObject("homeRunFrameDurationMillis", homeRunFrameDurationMillis);
        modelAndView.addObject("buntFrameDurationMillis", buntFrameDurationMillis);
        return modelAndView;
    }

    /**
     * トップ画面として、チーム別の既定オーダーを選び、打順をドラッグで組み替える打順組み替え画面を表示する。
     *
     * @return 打順組み替え画面
     */
    @GetMapping("/")
    public ModelAndView index() {
        var modelAndView = new ModelAndView("batting-order");
        modelAndView.addObject(
                "teams", defaultLineupQuery.findAllTeams().stream().map(this::toView).toList());
        return modelAndView;
    }

    /**
     * 空の打順入力欄を含む大規模実行画面を表示する。
     *
     * @return 大規模実行画面
     */
    @GetMapping("/large-scale")
    public ModelAndView largeScale() {
        return new ModelAndView("simulation");
    }

    private TeamLineupView toView(TeamDefaultLineup lineup) {
        List<DefaultBatterView> batters = lineup.batters().stream().map(this::toView).toList();
        return TeamLineupViewBuilder.teamLineupView()
                .key(lineup.team().name())
                .label(label(lineup.team()))
                .batters(batters)
                .build();
    }

    private DefaultBatterView toView(DefaultBatter batter) {
        return DefaultBatterViewBuilder.defaultBatterView()
                .battingOrder(batter.battingOrder())
                .hitAverage(String.format(Locale.ROOT, "%.3f", batter.hitAverage()))
                .personality(batter.personality().name())
                .stealForced(batter.stealForced())
                .buntForced(batter.buntForced())
                .build();
    }

    private String label(TeamStrength team) {
        return switch (team) {
            case STRONG -> "強";
            case AVERAGE -> "並";
            case WEAK -> "弱";
        };
    }
}

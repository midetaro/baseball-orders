package com.example.baseballorders.backend.infrastructure.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

/** 画面入力で打順設定を行うシミュレーション画面を提供する。 */
@Controller
public final class SimulationPageController {

    private final long frameDurationMillis;

    /**
     * 1試合実行結果のアニメーション表示に使うフレーム間隔を受け取って構成を作成する。
     *
     * @param frameDurationMillis 1フレームあたりの表示時間（ミリ秒）
     */
    public SimulationPageController(
            @Value("${baseball-orders.rendering.single-game.frame-duration-millis}")
                    long frameDurationMillis) {
        this.frameDurationMillis = frameDurationMillis;
    }

    /**
     * 空の打順入力欄を含む1試合実行画面を、アニメーション用フレーム間隔とともに表示する。
     *
     * @return 1試合実行画面
     */
    @GetMapping("/single-game")
    public ModelAndView singleGame() {
        var modelAndView = new ModelAndView("single-game");
        modelAndView.addObject("frameDurationMillis", frameDurationMillis);
        return modelAndView;
    }

    /**
     * トップ画面として大規模実行画面を表示する。
     *
     * @return 大規模実行画面
     */
    @GetMapping("/")
    public ModelAndView index() {
        return new ModelAndView("simulation");
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
}

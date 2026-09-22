package com.example.baseballorders.backend.infrastructure.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Googleアカウントでログインするための画面を提供する。 */
@Controller
public final class LoginPageController {

    /**
     * Googleログイン画面を表示する。
     *
     * @return ログイン画面のテンプレート名
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}

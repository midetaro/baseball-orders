package com.example.baseballorders.backend.infrastructure.web;

import com.example.baseballorders.backend.application.*;
import com.example.baseballorders.backend.infrastructure.security.SessionAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/** ログインとアカウント作成のHTMLフォームを提供する。 */
@Controller
@RequiredArgsConstructor
public final class UserAccountController {
    private final UserAccountService userAccountService;

    /** ログイン画面を表示する。 @return ログイン画面 */
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /** アカウント作成画面を表示する。 @return アカウント作成画面 */
    @GetMapping("/users/new")
    public String newUser() {
        return "user-new";
    }

    /**
     * アカウントを登録してログイン画面へ遷移する。
     *
     * @param username ログイン用ユーザー名
     * @param password 平文パスワード
     * @return 成功時はログイン画面、失敗時は登録画面
     */
    @PostMapping("/users")
    public String create(@RequestParam String username, @RequestParam String password) {
        try {
            userAccountService.register(username, password);
            return "redirect:/login?registered";
        } catch (IllegalArgumentException | DuplicateUsernameException exception) {
            return "redirect:/users/new?error";
        }
    }

    /**
     * 認証に成功した場合だけセッションへユーザーIDを保存する。
     *
     * @param username ログイン用ユーザー名
     * @param password 平文パスワード
     * @param request HTTP要求
     * @return 成功時はトップ、失敗時は統一エラー付きログイン画面
     */
    @PostMapping("/login")
    public String authenticate(
            @RequestParam String username,
            @RequestParam String password,
            HttpServletRequest request) {
        return userAccountService
                .authenticate(username, password)
                .map(
                        account -> {
                            request.getSession(true);
                            request.changeSessionId();
                            request.getSession()
                                    .setAttribute(
                                            SessionAuthenticationFilter.LOGIN_USER_ID,
                                            account.id());
                            return "redirect:/";
                        })
                .orElse("redirect:/login?error");
    }
}

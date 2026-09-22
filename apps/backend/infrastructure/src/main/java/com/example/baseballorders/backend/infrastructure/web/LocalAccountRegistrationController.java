package com.example.baseballorders.backend.infrastructure.web;

import com.example.baseballorders.backend.application.UserAccountService;
import java.nio.charset.StandardCharsets;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** ユーザー定義IDとパスワードによるローカルアカウント登録を受け付ける。 */
@Controller
public final class LocalAccountRegistrationController {
    private final UserAccountService userAccountService;
    private final PasswordEncoder passwordEncoder;

    /**
     * ローカルアカウント登録用コントローラーを作成する。
     *
     * @param userAccountService アカウント登録ユースケース
     * @param passwordEncoder パスワードハッシュ化サービス
     */
    public LocalAccountRegistrationController(
            UserAccountService userAccountService, PasswordEncoder passwordEncoder) {
        this.userAccountService = userAccountService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 新しいローカルアカウントを登録してログイン画面へ戻す。
     *
     * @param userId ユーザーが指定したログインID
     * @param password ユーザーが指定した平文パスワード
     * @return 登録結果を表すログイン画面へのリダイレクト
     */
    @PostMapping("/register")
    public String register(@RequestParam String userId, @RequestParam String password) {
        if (password == null
                || password.length() < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            return "redirect:/login?registrationError";
        }
        try {
            userAccountService.registerLocalAccount(userId, passwordEncoder.encode(password));
            return "redirect:/login?registered";
        } catch (IllegalArgumentException _) {
            return "redirect:/login?registrationError";
        }
    }
}

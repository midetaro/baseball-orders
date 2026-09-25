package com.example.baseballorders.backend.infrastructure.web;

import com.example.baseballorders.backend.application.UserAccountService;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** ユーザー定義IDとパスワードによるローカルアカウント登録を受け付ける。 */
@Controller
public final class LocalAccountRegistrationController {

    /** BCryptが受け付ける入力の固定上限であり、調整可能なパスワードポリシーではない。 */
    private static final int BCRYPT_MAXIMUM_PASSWORD_BYTES = 72;

    private final UserAccountService userAccountService;
    private final PasswordEncoder passwordEncoder;
    private final int minimumPasswordLength;

    /**
     * ローカルアカウント登録用コントローラーを作成する。
     *
     * @param userAccountService アカウント登録ユースケース
     * @param passwordEncoder パスワードハッシュ化サービス
     * @param minimumPasswordLength 設定で指定されたパスワードの最小文字数
     */
    public LocalAccountRegistrationController(
            UserAccountService userAccountService,
            PasswordEncoder passwordEncoder,
            @Value("${baseball-orders.security.local-account.minimum-password-length}")
                    int minimumPasswordLength) {
        this.userAccountService = userAccountService;
        this.passwordEncoder = passwordEncoder;
        this.minimumPasswordLength = minimumPasswordLength;
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
                || password.length() < minimumPasswordLength
                || password.getBytes(StandardCharsets.UTF_8).length
                        > BCRYPT_MAXIMUM_PASSWORD_BYTES) {
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

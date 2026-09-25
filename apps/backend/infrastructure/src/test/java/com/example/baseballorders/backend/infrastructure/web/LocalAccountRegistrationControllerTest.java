package com.example.baseballorders.backend.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.baseballorders.backend.application.UserAccountRepository;
import com.example.baseballorders.backend.application.UserAccountService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class LocalAccountRegistrationControllerTest {

    private static final PasswordEncoder PASSWORD_ENCODER =
            new PasswordEncoder() {
                @Override
                public String encode(CharSequence rawPassword) {
                    return "hashed:" + rawPassword;
                }

                @Override
                public boolean matches(CharSequence rawPassword, String encodedPassword) {
                    return encode(rawPassword).equals(encodedPassword);
                }
            };

    private static LocalAccountRegistrationController controller(int minimumPasswordLength) {
        // 未登録のアカウント永続化ポートを表すモック。findByUsernameは既定でOptional.emptyを返す。
        UserAccountRepository repository = org.mockito.Mockito.mock(UserAccountRepository.class);
        return new LocalAccountRegistrationController(
                new UserAccountService(repository), PASSWORD_ENCODER, minimumPasswordLength);
    }

    @Test
    @DisplayName("設定した最小文字数と同じ長さのパスワードは登録できる")
    void registersPasswordAtConfiguredMinimumLength() {
        // given
        var sut = controller(8);

        // when
        var view = sut.register("local-user", "12345678");

        // then
        assertAll(() -> assertEquals("redirect:/login?registered", view));
    }

    @Test
    @DisplayName("設定した最小文字数を下回るパスワードは登録エラーにする")
    void rejectsPasswordBelowConfiguredMinimumLength() {
        // given
        var sut = controller(8);

        // when
        var view = sut.register("local-user", "1234567");

        // then
        assertAll(() -> assertEquals("redirect:/login?registrationError", view));
    }

    @Test
    @DisplayName("最小文字数を12に設定すると11文字のパスワードは登録エラーにする")
    void rejectsPasswordBelowRaisedMinimumLength() {
        // given
        var sut = controller(12);

        // when
        var view = sut.register("local-user", "12345678901");

        // then
        assertAll(() -> assertEquals("redirect:/login?registrationError", view));
    }

    @Test
    @DisplayName("BCryptの固定上限である72バイトを超えるパスワードは登録エラーにする")
    void rejectsPasswordAboveBcryptFixedByteLimit() {
        // given
        var sut = controller(8);
        var password = "a".repeat(73);

        // when
        var view = sut.register("local-user", password);

        // then
        assertAll(() -> assertEquals("redirect:/login?registrationError", view));
    }
}

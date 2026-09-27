package com.example.baseballorders.backend.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LoginPageControllerTest {

    @Test
    @DisplayName("ログイン画面を表示するとログイン用テンプレートを返す")
    void showsLoginPage() {
        // given
        var sut = new LoginPageController();

        // when
        var page = sut.login();

        // then
        assertAll(() -> assertEquals("login", page));
    }
}

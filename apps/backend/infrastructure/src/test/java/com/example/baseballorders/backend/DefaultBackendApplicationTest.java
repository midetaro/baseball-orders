package com.example.baseballorders.backend;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class DefaultBackendApplicationTest {

    @Test
    @DisplayName("プロファイル未指定ではlocal設定でSQSリスナーの自動起動を停止する")
    void defaultsToLocalProfile() {
        // given
        var runner =
                new ApplicationContextRunner()
                        .withInitializer(new ConfigDataApplicationContextInitializer());

        // when
        runner.run(
                context -> {
                    var environment = context.getEnvironment();

                    // then
                    assertAll(
                            () -> assertEquals("local", environment.getDefaultProfiles()[0]),
                            () ->
                                    assertEquals(
                                            false,
                                            environment.getProperty(
                                                    "spring.cloud.aws.sqs.listener.auto-startup",
                                                    Boolean.class,
                                                    true)));
                });
    }

    @Test
    @DisplayName("awsプロファイルを明示した場合はSQSリスナーの自動起動を維持する")
    void explicitAwsProfileKeepsListenersEnabled() {
        // given
        var runner =
                new ApplicationContextRunner()
                        .withPropertyValues("spring.profiles.active=aws")
                        .withInitializer(new ConfigDataApplicationContextInitializer());

        // when
        runner.run(
                context -> {
                    var environment = context.getEnvironment();

                    // then
                    assertAll(
                            () -> assertEquals("aws", environment.getActiveProfiles()[0]),
                            () ->
                                    assertTrue(
                                            environment.getProperty(
                                                    "spring.cloud.aws.sqs.listener.auto-startup",
                                                    Boolean.class,
                                                    true)));
                });
    }
}

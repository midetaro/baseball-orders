package com.example.baseballorders.backend;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class DefaultBackendApplicationTest {

    @Test
    @DisplayName("プロファイル未指定ではlocal設定でローカルのsimulatorへ接続する")
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
                                            "localhost",
                                            environment.getProperty(
                                                    "baseball-orders.simulator.grpc.host")),
                            () ->
                                    assertEquals(
                                            9090,
                                            environment.getProperty(
                                                    "baseball-orders.simulator.grpc.port",
                                                    Integer.class)));
                });
    }

    @ParameterizedTest
    @CsvSource({"local,localhost,9090", "dev,simulator,9090", "prod,simulator,9090"})
    @DisplayName("各プロファイルはsimulatorのgRPC接続先を明示的に持つ")
    void eachProfileDefinesSimulatorAddress(String profile, String host, int port) {
        // given
        var runner =
                new ApplicationContextRunner()
                        .withPropertyValues("spring.profiles.active=" + profile)
                        .withInitializer(new ConfigDataApplicationContextInitializer());

        // when
        runner.run(
                context -> {
                    var environment = context.getEnvironment();

                    // then
                    assertAll(
                            () -> assertEquals(profile, environment.getActiveProfiles()[0]),
                            () ->
                                    assertEquals(
                                            host,
                                            environment.getProperty(
                                                    "baseball-orders.simulator.grpc.host")),
                            () ->
                                    assertEquals(
                                            port,
                                            environment.getProperty(
                                                    "baseball-orders.simulator.grpc.port",
                                                    Integer.class)));
                });
    }

    @Test
    @DisplayName("SIMULATOR_GRPC_HOSTとSIMULATOR_GRPC_PORTでsimulatorの接続先を上書きできる")
    void environmentOverridesSimulatorAddress() {
        // given
        var runner =
                new ApplicationContextRunner()
                        .withPropertyValues(
                                "spring.profiles.active=prod",
                                "SIMULATOR_GRPC_HOST=simulator.internal",
                                "SIMULATOR_GRPC_PORT=19090")
                        .withInitializer(new ConfigDataApplicationContextInitializer());

        // when
        runner.run(
                context -> {
                    var environment = context.getEnvironment();

                    // then
                    assertAll(
                            () ->
                                    assertEquals(
                                            "simulator.internal",
                                            environment.getProperty(
                                                    "baseball-orders.simulator.grpc.host")),
                            () ->
                                    assertEquals(
                                            19090,
                                            environment.getProperty(
                                                    "baseball-orders.simulator.grpc.port",
                                                    Integer.class)));
                });
    }
}

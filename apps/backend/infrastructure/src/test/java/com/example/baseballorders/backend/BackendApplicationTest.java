package com.example.baseballorders.backend;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.example.baseballorders.backend.application.SimulationCoordinator;
import com.example.baseballorders.backend.application.adapter.SimulatorClient;
import com.example.baseballorders.backend.infrastructure.api.SimulatorRequestController;
import com.example.baseballorders.backend.infrastructure.messaging.GrpcSimulatorClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BackendApplicationTest {

    @Autowired private SimulatorRequestController controller;
    @Autowired private SimulationCoordinator coordinator;
    @Autowired private SimulatorClient simulatorClient;

    @Test
    @DisplayName("backendはUIを除くシミュレーション部品をSpring Bootで起動できる")
    void startsBackendComponents() {
        // given

        // when

        // then
        assertAll(
                () -> assertNotNull(controller),
                () -> assertNotNull(coordinator),
                () -> assertInstanceOf(GrpcSimulatorClient.class, simulatorClient));
    }
}

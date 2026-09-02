package com.example.baseballorders.backend;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.example.baseballorders.backend.simulation.infrastructure.api.SimulatorRequestController;
import com.example.baseballorders.backend.simulation.application.SimulationCoordinator;
import com.example.baseballorders.backend.simulation.infrastructure.messaging.SimulationResultListener;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = "spring.cloud.aws.sqs.enabled=false")
class BackendApplicationTest {

    @MockitoBean private SqsTemplate sqsTemplate;

    @Autowired private SimulatorRequestController controller;
    @Autowired private SimulationCoordinator coordinator;
    @Autowired private SimulationResultListener listener;

    @Test
    @DisplayName("backendはUIを除くシミュレーション部品をSpring Bootで起動できる")
    void startsBackendComponents() {
        // given

        // when

        // then
        assertAll(
                () -> assertNotNull(controller),
                () -> assertNotNull(coordinator),
                () -> assertNotNull(listener));
    }
}

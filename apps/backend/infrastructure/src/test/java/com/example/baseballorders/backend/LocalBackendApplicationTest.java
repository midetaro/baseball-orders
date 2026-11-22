package com.example.baseballorders.backend;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")
class LocalBackendApplicationTest {

    @Autowired private ApplicationContext context;

    @Test
    @DisplayName("localプロファイルではsimulatorへ接続せずにgRPCクライアントを構成して起動できる")
    void startsWithoutSimulatorConnection() {
        // given

        // when
        boolean clientExists = context.containsBean("grpcSimulatorClient");

        // then
        assertAll(() -> assertTrue(clientExists));
    }
}

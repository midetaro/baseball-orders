package com.example.baseballorders.backend;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:local-backend-startup")
@ActiveProfiles("local")
class LocalBackendApplicationTest {

    @Autowired private ApplicationContext context;

    @Test
    @DisplayName("localプロファイルではSQSへの接続なしで起動できる")
    void startsWithoutSqsConnection() {
        // given

        // when
        boolean publisherExists = context.containsBean("sqsSimulatorMessagePublisher");

        // then
        assertAll(() -> assertTrue(publisherExists));
    }
}

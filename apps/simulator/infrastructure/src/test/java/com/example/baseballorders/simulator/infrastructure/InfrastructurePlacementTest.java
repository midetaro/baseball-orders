package com.example.baseballorders.simulator.infrastructure;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.baseballorders.simulator.infrastructure.config.SimulationInfrastructureConfiguration;
import com.example.baseballorders.simulator.infrastructure.messaging.LineUpMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InfrastructurePlacementTest {

    @Test
    @DisplayName("SQS境界mapperとインフラ設定はinfrastructureに配置する")
    void placesSqsBoundaryTypesInInfrastructure() {
        // given
        var expectedConfigurationPackage =
                "com.example.baseballorders.simulator.infrastructure.config";
        var expectedMapperPackage = "com.example.baseballorders.simulator.infrastructure.messaging";

        // when
        var configurationPackage = SimulationInfrastructureConfiguration.class.getPackageName();
        var mapperPackage = LineUpMapper.class.getPackageName();

        // then
        assertAll(
                () -> assertEquals(expectedConfigurationPackage, configurationPackage),
                () -> assertEquals(expectedMapperPackage, mapperPackage));
    }
}

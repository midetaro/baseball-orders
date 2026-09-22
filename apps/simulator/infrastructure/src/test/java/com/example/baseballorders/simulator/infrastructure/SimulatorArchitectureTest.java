package com.example.baseballorders.simulator.infrastructure;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SimulatorArchitectureTest {

    private static final String DOMAIN = "com.example.baseballorders.simulator.domain..";
    private static final String APPLICATION = "com.example.baseballorders.simulator.application..";
    private static final String INFRASTRUCTURE =
            "com.example.baseballorders.simulator.infrastructure..";

    @Test
    @DisplayName("シミュレーターの依存方向とドメインの技術非依存を守る")
    void enforcesModuleBoundaries() {
        // given
        var classes =
                new ClassFileImporter().importPackages("com.example.baseballorders.simulator");
        var domainRule =
                noClasses()
                        .that()
                        .resideInAPackage(DOMAIN)
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage(
                                APPLICATION,
                                INFRASTRUCTURE,
                                "org.springframework..",
                                "software.amazon..",
                                "com.fasterxml..",
                                "com.example.baseballorders.messaging..",
                                "com.example.baseballorders.backend..",
                                "java.net.http..",
                                "java.sql..",
                                "javax.sql..",
                                "javax.persistence..",
                                "jakarta.persistence..",
                                "javax.servlet..",
                                "jakarta.servlet..",
                                "javax.ws.rs..",
                                "jakarta.ws.rs..",
                                "org.hibernate..",
                                "org.jooq..",
                                "org.thymeleaf..",
                                "org.apache.http..",
                                "org.apache.hc..",
                                "okhttp3..",
                                "retrofit2..");
        var applicationRule =
                noClasses()
                        .that()
                        .resideInAPackage(APPLICATION)
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage(
                                INFRASTRUCTURE,
                                "com.example.baseballorders.backend..",
                                "com.example.baseballorders.messaging..",
                                "software.amazon..",
                                "com.fasterxml..");
        var infrastructureRule =
                noClasses()
                        .that()
                        .resideInAPackage(INFRASTRUCTURE)
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage("com.example.baseballorders.backend..");

        // when
        domainRule.check(classes);
        applicationRule.check(classes);
        infrastructureRule.check(classes);

        // then
        assertAll(
                () ->
                        assertTrue(
                                classes.stream()
                                        .anyMatch(
                                                type ->
                                                        type.getPackageName()
                                                                .contains(".domain."))),
                () ->
                        assertTrue(
                                classes.stream()
                                        .anyMatch(
                                                type ->
                                                        type.getPackageName()
                                                                .contains(".application."))),
                () ->
                        assertTrue(
                                classes.stream()
                                        .anyMatch(
                                                type ->
                                                        type.getPackageName()
                                                                .contains(".infrastructure."))));
    }
}

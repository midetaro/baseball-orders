package com.example.baseballorders.backend.infrastructure.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InfrastructureArchitectureTest {

    private static final String INFRASTRUCTURE_PACKAGE =
            "com.example.baseballorders.backend.infrastructure";
    private static final String API_PACKAGE = INFRASTRUCTURE_PACKAGE + ".api..";
    private static final String WEB_PACKAGE = INFRASTRUCTURE_PACKAGE + ".web..";
    private static final String MESSAGING_PACKAGE = INFRASTRUCTURE_PACKAGE + ".messaging..";
    private static final String PERSISTENCE_PACKAGE = INFRASTRUCTURE_PACKAGE + ".persistence..";
    private static final JavaClasses INFRASTRUCTURE_CLASSES =
            new ClassFileImporter()
                    .withImportOption(new ImportOption.DoNotIncludeTests())
                    .importPackages(INFRASTRUCTURE_PACKAGE);

    @Test
    @DisplayName("infrastructureアダプタは別アダプタの実装に直接依存しない")
    void adaptersDoNotDependOnOtherAdapters() {
        // given
        ArchRule apiDoesNotDependOnOtherAdapters =
                hasNoDependenciesOn(
                        API_PACKAGE, WEB_PACKAGE, MESSAGING_PACKAGE, PERSISTENCE_PACKAGE);
        ArchRule webDoesNotDependOnOtherAdapters =
                hasNoDependenciesOn(
                        WEB_PACKAGE, API_PACKAGE, MESSAGING_PACKAGE, PERSISTENCE_PACKAGE);
        ArchRule messagingDoesNotDependOnOtherAdapters =
                hasNoDependenciesOn(
                        MESSAGING_PACKAGE, API_PACKAGE, WEB_PACKAGE, PERSISTENCE_PACKAGE);
        ArchRule persistenceDoesNotDependOnOtherAdapters =
                hasNoDependenciesOn(
                        PERSISTENCE_PACKAGE, API_PACKAGE, WEB_PACKAGE, MESSAGING_PACKAGE);

        // when
        // then
        assertAll(
                () -> apiDoesNotDependOnOtherAdapters.check(INFRASTRUCTURE_CLASSES),
                () -> webDoesNotDependOnOtherAdapters.check(INFRASTRUCTURE_CLASSES),
                () -> messagingDoesNotDependOnOtherAdapters.check(INFRASTRUCTURE_CLASSES),
                () -> persistenceDoesNotDependOnOtherAdapters.check(INFRASTRUCTURE_CLASSES));
    }

    @Test
    @DisplayName("JPAクライアントはpersistenceアダプタからのみ参照できる")
    void onlyPersistenceAdapterDependsOnJpa() {
        // given
        ArchRule nonPersistenceAdaptersDoNotDependOnJpa =
                noClasses()
                        .that()
                        .resideInAnyPackage(API_PACKAGE, WEB_PACKAGE, MESSAGING_PACKAGE)
                        .should()
                        .dependOnClassesThat()
                        .resideInAPackage("jakarta.persistence..");

        // when
        // then
        assertAll(() -> nonPersistenceAdaptersDoNotDependOnJpa.check(INFRASTRUCTURE_CLASSES));
    }

    private static ArchRule hasNoDependenciesOn(String sourcePackage, String... targetPackages) {
        return noClasses()
                .that()
                .resideInAPackage(sourcePackage)
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(targetPackages);
    }
}

package com.kidwany.casestudies.distributedlockfailure;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
        packages = "com.kidwany.casestudies.distributedlockfailure",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_depends_on_nothing = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..application..", "..adapter..", "..config..",
                    "org.springframework..", "jakarta.persistence..",
                    "jakarta.validation..", "com.fasterxml.jackson..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule application_does_not_know_adapters = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage("..adapter..", "..config..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule inbound_adapters_use_only_input_ports = noClasses()
            .that().resideInAPackage("..adapter.in..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..application.service..", "..application.port.out..", "..adapter.out..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule adapters_are_isolated = slices()
            .matching("..adapter.(*).(*)..")
            .should().notDependOnEachOther()
            .allowEmptyShould(true);
}

package com.casestudies.payoutdelivery.fakebank;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
        packages = "com.casestudies.payoutdelivery.fakebank",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String[] REDIS = {
            "org.springframework.data.redis..", "io.lettuce..", "redis.clients..", "org.redisson.."};

    private static final String[] PERSISTENCE = {
            "jakarta.persistence..", "jakarta.transaction..", "org.hibernate..",
            "org.springframework.data..", "org.flywaydb.."};

    private static final String[] KAFKA = {"org.apache.kafka..", "org.springframework.kafka.."};

    private static final String[] HTTP = {
            "java.net.http..", "jakarta.servlet..", "org.springframework.web..",
            "org.springframework.http..", "org.apache.hc..", "okhttp3.."};

    @ArchTest
    static final ArchRule dependencies_point_inward = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .withOptionalLayers(true)
            .layer("Domain").definedBy("..domain..")
            .layer("Application").definedBy("..application..")
            .layer("Adapters").definedBy("..adapter..")
            .layer("Config").definedBy("..config..")
            .whereLayer("Adapters").mayOnlyBeAccessedByLayers("Config")
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapters", "Config")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Adapters", "Config");

    @ArchTest
    static final ArchRule domain_has_no_spring_dependencies = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domain_has_no_redis_dependencies = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(REDIS)
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domain_has_no_persistence_dependencies = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(PERSISTENCE)
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domain_has_no_kafka_dependencies = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(KAFKA)
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domain_has_no_http_dependencies = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(HTTP)
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domain_has_no_serialization_or_validation_annotations = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta.validation..", "com.fasterxml.jackson..", "tools.jackson..")
            .allowEmptyShould(true);

    // Value objects are immutable: records and enums pass, a class with a mutable field does not.
    @ArchTest
    static final ArchRule value_objects_are_immutable = classes()
            .that().resideInAPackage("..domain.valueobject..")
            .should().haveOnlyFinalFields()
            .allowEmptyShould(true);

    // Aggregates are composed of value objects, never the other way round.
    @ArchTest
    static final ArchRule value_objects_do_not_know_aggregates = noClasses()
            .that().resideInAPackage("..domain.valueobject..")
            .should().dependOnClassesThat().resideInAPackage("..domain.aggregate..")
            .allowEmptyShould(true);

    // Aggregate state changes go through intention-revealing methods (startDispatch), not setters.
    @ArchTest
    static final ArchRule aggregates_have_no_public_setters = noMethods()
            .that().areDeclaredInClassesThat().resideInAPackage("..domain.aggregate..")
            .and().arePublic()
            .should().haveNameMatching("set[A-Z].*")
            .allowEmptyShould(true);

    // @Transactional on a use case is the one framework import the application layer gets.
    @ArchTest
    static final ArchRule application_reaches_infrastructure_only_through_ports = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat(
                    resideInAnyPackage("org.springframework..", "jakarta.persistence..",
                            "org.hibernate..", "io.lettuce..", "redis.clients..", "org.redisson..",
                            "org.apache.kafka..", "java.net.http..", "jakarta.servlet..",
                            "org.apache.hc..", "okhttp3..")
                            .and(not(resideInAPackage("org.springframework.transaction.annotation.."))))
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule infrastructure_depends_inward_through_ports = noClasses()
            .that().resideInAPackage("..adapter.out..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..application.service..", "..application.port.in..", "..config..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule inbound_adapters_use_only_input_ports = noClasses()
            .that().resideInAPackage("..adapter.in..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..application.service..", "..application.port.out..", "..adapter.out..")
            .allowEmptyShould(true);

    // Controllers map DTO -> command -> one use case -> DTO. Anything that would let them
    // do the work themselves (domain services, repositories, transactions, Redis) is off limits.
    @ArchTest
    static final ArchRule controllers_hold_no_business_logic = noClasses()
            .that().resideInAPackage("..adapter.in.api.controller..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..domain.service..", "..application.service..", "..application.port.out..",
                    "..adapter.out..", "org.springframework.transaction..",
                    "jakarta.persistence..", "org.springframework.data..", "io.lettuce..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule adapters_are_isolated = slices()
            .matching("..adapter.(*).(*)..")
            .should().notDependOnEachOther()
            .allowEmptyShould(true);
}

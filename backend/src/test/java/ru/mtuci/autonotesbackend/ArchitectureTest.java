package ru.mtuci.autonotesbackend;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

public class ArchitectureTest {

    private final JavaClasses importedClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("ru.mtuci.autonotesbackend");

    @Test
    void controllers_should_only_use_facades_or_services() {
        classes()
                .that()
                .haveSimpleNameEndingWith("Controller")
                .should()
                .onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "ru.mtuci.autonotesbackend.modules..api..",
                        "ru.mtuci.autonotesbackend.security..",
                        "java..",
                        "javax..",
                        "jakarta..",
                        "org.springframework..",
                        "ru.mtuci.autonotesbackend.exception..",
                        "lombok..",
                        "org.slf4j..",
                        "io.swagger.v3.oas.annotations..",
                        "io.github.resilience4j..")
                .check(importedClasses);
    }

    @Test
    void controllers_should_reside_in_modules_api_controller() {
        classes()
                .that()
                .haveSimpleNameEndingWith("Controller")
                .should()
                .resideInAPackage("..modules..api.controller..")
                .check(importedClasses);
    }

    @Test
    void notes_module_isolation() {
        noClasses()
                .that()
                .resideInAPackage("..modules.notes.impl..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..modules.filestorage.impl..")
                .check(importedClasses);
    }

    @Test
    void facades_should_not_return_entities() {
        noClasses()
                .that()
                .haveSimpleNameEndingWith("Facade")
                .should()
                .dependOnClassesThat()
                .areAnnotatedWith("jakarta.persistence.Entity")
                .check(importedClasses);
    }
}

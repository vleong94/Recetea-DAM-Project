package com.recetea.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Enforces the Hexagonal Architecture (Ports & Adapters) boundaries defined in CLAUDE.md.
 *
 * Layer model:
 *   Domain      → core..domain..
 *   Application → core..application..  (ports.in / ports.out / usecases)
 *   Infrastructure → infrastructure..
 *
 * Allowed dependency directions: Infrastructure → Application → Domain (inward only).
 */
@AnalyzeClasses(packages = "com.recetea", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    // ── Rule 1: Domain Isolation ───────────────────────────────────────────────
    // The domain layer is the innermost ring: it must be a pure POJO model with
    // no knowledge of how it is persisted, exposed, or orchestrated.

    @ArchTest
    static final ArchRule domain_must_not_depend_on_application_or_infrastructure =
            noClasses()
                    .that().resideInAPackage("com.recetea.core..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.recetea.core..application..",
                            "com.recetea.infrastructure.."
                    )
                    .as("Domain must not depend on Application or Infrastructure layers");

    // ── Rule 2: Application Isolation ─────────────────────────────────────────
    // Use cases and ports may call into the domain and reference each other, but
    // must never import any infrastructure class — that would invert the dependency
    // and couple business logic to implementation details.

    @ArchTest
    static final ArchRule application_must_not_depend_on_infrastructure =
            noClasses()
                    .that().resideInAPackage("com.recetea.core..application..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("com.recetea.infrastructure..")
                    .as("Application (use cases & ports) must not depend on Infrastructure");

    // ── Rule 3: UI through Inbound Ports Only ─────────────────────────────────
    // Controllers may depend on domain VOs/exceptions (cross-cutting concerns) and
    // other infrastructure classes (StorageConfig, XmlInteropAdapter exception, etc.),
    // but must never reference:
    //   • Use case implementations — always program to the IXxxUseCase interface.
    //   • Outbound ports (ports.out) — those are contracts for infrastructure adapters,
    //     not for the UI layer.

    @ArchTest
    static final ArchRule ui_must_not_bypass_application_ports =
            noClasses()
                    .that().resideInAPackage("com.recetea.infrastructure.ui..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.recetea.core..application..usecases..",
                            "com.recetea.core..application..ports.out.."
                    )
                    .as("UI controllers must program to inbound port interfaces (ports.in), " +
                        "never to use case implementations or outbound ports");

    // ── Rule 4: Use Case Naming Convention ────────────────────────────────────
    // Every public class that lives in a usecases sub-package is a use case
    // implementation and must be named with the "UseCase" suffix so it is
    // immediately recognisable. Package-private helpers (e.g. RecipeResponseMapper)
    // are intentionally excluded from this convention.

    @ArchTest
    static final ArchRule public_classes_in_usecases_must_be_named_use_case =
            classes()
                    .that().resideInAPackage("com.recetea.core..application..usecases..")
                    .and().arePublic()
                    .should().haveSimpleNameEndingWith("UseCase")
                    .as("Public classes in usecases packages must have names ending with 'UseCase'");
}

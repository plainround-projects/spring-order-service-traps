package dev.plainround.orders;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.transaction.annotation.Transactional;

/**
 * The dependency rule and the two proxy rules from the lesson, as code: they fail the build, not
 * the review.
 */
@AnalyzeClasses(
        packages = "dev.plainround.orders",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule layersOnlyDependInward =
            layeredArchitecture()
                    .consideringOnlyDependenciesInLayers()
                    .layer("Domain")
                    .definedBy("..domain..")
                    .layer("Application")
                    .definedBy("..application..")
                    .layer("Infrastructure")
                    .definedBy("..infrastructure..")
                    .layer("Interfaces")
                    .definedBy("..interfaces..")
                    .whereLayer("Interfaces")
                    .mayNotBeAccessedByAnyLayer()
                    .whereLayer("Infrastructure")
                    .mayNotBeAccessedByAnyLayer()
                    .whereLayer("Application")
                    .mayOnlyBeAccessedByLayers("Interfaces")
                    .whereLayer("Domain")
                    .mayOnlyBeAccessedByLayers("Application", "Infrastructure", "Interfaces");

    @ArchTest
    static final ArchRule domainDependsOnNothingButTheJdk =
            noClasses()
                    .that()
                    .resideInAPackage("..domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideOutsideOfPackages("..domain..", "java..");

    @ArchTest
    static final ArchRule applicationKnowsOnlyTheDomainTheJdkLoggingAndTransactions =
            noClasses()
                    .that()
                    .resideInAPackage("..application..")
                    .should()
                    .dependOnClassesThat()
                    .resideOutsideOfPackages(
                            "..domain..",
                            "..application..",
                            "java..",
                            "org.slf4j..",
                            "org.springframework.stereotype..",
                            "org.springframework.transaction.annotation..");

    @ArchTest
    static final ArchRule persistenceEntitiesStayInsideTheirPackage =
            noClasses()
                    .that()
                    .resideOutsideOfPackage("..infrastructure.persistence..")
                    .should()
                    .dependOnClassesThat(
                            JavaClass.Predicates.resideInAPackage("..infrastructure.persistence..")
                                    .and(JavaClass.Predicates.simpleNameEndingWith("Entity")));

    @ArchTest
    static final ArchRule noFieldInjection =
            noFields()
                    .should()
                    .beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
                    .orShould()
                    .beAnnotatedWith("jakarta.inject.Inject")
                    .orShould()
                    .beAnnotatedWith("jakarta.annotation.Resource");

    @ArchTest
    static final ArchRule controllersLiveInTheInterfacesLayer =
            classes()
                    .that()
                    .areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                    .should()
                    .resideInAPackage("..interfaces..");

    @ArchTest
    static final ArchRule adaptersLiveInInfrastructure =
            classes()
                    .that()
                    .areAnnotatedWith("org.springframework.stereotype.Repository")
                    .should()
                    .resideInAPackage("..infrastructure..");

    @ArchTest
    static final ArchRule noPackageCycles =
            slices().matching("dev.plainround.orders.(*)..").should().beFreeOfCycles();

    /**
     * Trap 2: a call from one method of a class to a {@code @Transactional} method of the same
     * class skips the proxy.
     */
    @ArchTest
    static final ArchRule noSelfInvocationOfTransactionalMethods =
            noClasses().should(callATransactionalMethodOfTheirOwnClass());

    /** A private method is never proxied, so a {@code @Transactional} on it is silently ignored. */
    @ArchTest
    static final ArchRule transactionalIsNeverOnAPrivateMethod =
            methods().that().arePrivate().should().notBeAnnotatedWith(Transactional.class);

    private static ArchCondition<JavaClass> callATransactionalMethodOfTheirOwnClass() {
        return new ArchCondition<>("call a @Transactional method of their own class") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                for (JavaMethodCall call : javaClass.getMethodCallsFromSelf()) {
                    boolean sameClass = call.getTargetOwner().equals(javaClass);
                    boolean transactional =
                            call.getTarget()
                                    .resolveMember()
                                    .map(method -> method.isAnnotatedWith(Transactional.class))
                                    .orElse(false);
                    if (sameClass && transactional) {
                        events.add(SimpleConditionEvent.satisfied(call, call.getDescription()));
                    }
                }
            }
        };
    }
}

# ArchUnit

## What it is

ArchUnit is a library that lets you write rules about the structure of your code as ordinary tests: "classes in `domain` may not depend on anything outside `domain`", "no field may be annotated `@Autowired`". It reads the compiled `.class` files, builds a model of who uses whom, and checks the rules against it.

## Why this repository uses it

The project rules say the layers are `domain`, `application`, `infrastructure` and `interfaces`, and that dependencies point inward: domain depends on nothing, application depends on domain, infrastructure and interfaces depend on both, and nothing depends on interfaces. Written in a document, a rule like that is forgotten. As a test, it fails the build the day someone breaks it.

## Where it appears

| Path | Role |
| --- | --- |
| `src/test/java/dev/plainround/orders/ArchitectureTest.java` | the rules, plus two proxy rules for transactions |
| `pom.xml` | the `archunit-junit5` dependency, test scope |

## How it works underneath

1. `@AnalyzeClasses(packages = "...", importOptions = DoNotIncludeTests.class)` tells ArchUnit which compiled classes to import; here only production classes.
2. Each `@ArchTest static final ArchRule ...` field is one test. JUnit runs it through ArchUnit's engine.
3. A rule is built with a fluent API (`noClasses().that()...should()...`). When run, ArchUnit looks at every class, method, field and call, and reports each violation with the class and line.
4. A **layered architecture** rule names layers by package pattern (`..domain..`) and states who may access whom.
5. A custom `ArchCondition` can inspect method calls, which is how the proxy rules find a call from a class to a `@Transactional` method of the same class.

## Walkthrough of this repository's use

```java
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
```

The first rule is the layered rule. `consideringOnlyDependenciesInLayers()` ignores dependencies on libraries (Spring, the JDK). Four layers are defined by package. `Interfaces` and `Infrastructure` "may not be accessed by any layer": nothing in the project depends on them. `Application` may be accessed only by `Interfaces`. `Domain` may be accessed by the three outer layers.

```java
static final ArchRule domainDependsOnNothingButTheJdk =
        noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat()
                .resideOutsideOfPackages("..domain..", "java..");
```

The second rule is stricter than the first: a class in `domain` may depend only on `domain` and `java..` packages. No Spring, no JPA, no Jackson in the domain.

```java
static final ArchRule noFieldInjection =
        noFields()
                .should()
                .beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
                .orShould()
                .beAnnotatedWith("jakarta.inject.Inject")
                .orShould()
                .beAnnotatedWith("jakarta.annotation.Resource");
```

No field may carry `@Autowired`, `@Inject` or `@Resource`: constructor injection only.

```java
static final ArchRule persistenceEntitiesStayInsideTheirPackage =
        noClasses()
                .that()
                .resideOutsideOfPackage("..infrastructure.persistence..")
                .should()
                .dependOnClassesThat(
                        JavaClass.Predicates.resideInAPackage("..infrastructure.persistence..")
                                .and(JavaClass.Predicates.simpleNameEndingWith("Entity")));
```

The JPA entities (classes named `*Entity` in `infrastructure.persistence`) may be used only inside that package: adapters turn them into domain records before anything else sees them.

```java
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
```

This is the custom condition for the second trap. It looks at every method call a class makes, and flags a call whose target is a `@Transactional` method of **the same class**, because such a call skips the Spring proxy and the transaction never starts. A second rule forbids `@Transactional` on private methods for the same reason.

**The test class that holds the ArchUnit rules, with its rule names:**

- `src/test/java/dev/plainround/orders/ArchitectureTest.java`:
  - `layersOnlyDependInward`
  - `domainDependsOnNothingButTheJdk`
  - `applicationKnowsOnlyTheDomainTheJdkLoggingAndTransactions`
  - `persistenceEntitiesStayInsideTheirPackage`
  - `noFieldInjection`
  - `controllersLiveInTheInterfacesLayer`
  - `adaptersLiveInInfrastructure`
  - `noPackageCycles`
  - `noSelfInvocationOfTransactionalMethods`
  - `transactionalIsNeverOnAPrivateMethod`

## Try it yourself

1. `./mvnw test -Dtest=ArchitectureTest` runs the rules.
2. Break a rule on purpose: add `import org.springframework.stereotype.Component;` to a class in `domain/` and run the test again. It names the class and the dependency.
3. Add `@Autowired` to a field and run again.

## Mistakes people make

- Writing rules that match nothing (a typo in a package name): the rule passes while checking nothing. This repository keeps rules on real packages and breaks one to prove it works.
- Importing test classes into the analysis: tests are allowed to break rules on purpose (the traps do), so they are excluded.
- Using it instead of review: it catches structure, not logic.

## Check yourself

1. What does ArchUnit read? *The compiled classes.*
2. What does the layered rule forbid? *A layer using a layer it may not depend on.*
3. Why is `domain` stricter than the layered rule? *It forbids framework dependencies too.*
4. Why are test classes excluded? *The traps in tests break rules on purpose.*
5. What does the custom condition detect? *A call to a proxied method of the same class.*

## Further reading

- ArchUnit User Guide (version 1.5.1): https://www.archunit.org/userguide/html/000_Index.html

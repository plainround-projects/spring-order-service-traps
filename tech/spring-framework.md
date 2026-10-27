# Spring Framework (beans, injection, proxies)

## What it is

Spring Framework is a library that creates and connects the objects of a Java program. Three ideas carry it.

- A **bean** is an object that Spring creates and keeps. The collection of all beans is the **application context**.
- **Dependency injection** (DI): a class does not create the objects it needs. It asks for them in its **constructor** (the method that builds the object), and Spring passes them in.
- A **proxy** is a stand-in object. Spring wraps some beans in a proxy that adds behavior (a database transaction, a retry, a limit) around the real method call.

## Why this repository uses it

The whole project is a Spring project. Constructor injection is a rule of the lesson: each class lists what it depends on in one constructor, so the dependencies are visible and a test can pass fakes. The proxy is also the subject of the lesson's hardest ideas: it decides when `@Transactional`, `@Retryable` and `@ConcurrencyLimit` apply, and when they silently do not.

## Where it appears

| Path | Role |
| --- | --- |
| `application/CheckoutService.java` and the other services | `@Service` beans with one constructor each |
| `infrastructure/persistence/Jpa*Repository.java` | `@Repository` beans that implement the domain's interfaces |
| `infrastructure/persistence/JpaAuditLog.java` | a `@Component` that must stay a separate bean (see `spring-transactions.md`) |
| `infrastructure/ClockConfig.java` | a `@Configuration` class with one `@Bean` method that creates the system clock |

## How it works underneath

1. At startup Spring scans the packages under the class marked `@SpringBootApplication`, finds classes with a **stereotype annotation** (`@Component`, `@Service`, `@Repository`, `@RestController`, `@Configuration`) and registers each as a bean definition.
2. For each definition Spring works out the constructor, finds beans for each parameter by **type**, builds those first, and then builds the object. If a type has no bean, startup fails with a message that names it.
3. A class with exactly one constructor needs no `@Autowired`. This repository never uses `@Autowired` on fields, and a test (`ArchitectureTest`) fails if someone does.
4. `@Bean` on a method inside a `@Configuration` class means "the object this method returns is a bean". It is how the code registers objects it cannot annotate, such as `Clock.systemUTC()`.
5. When a bean's class has an annotation that needs a proxy, Spring builds the real object and then builds a **subclass at run time** (a CGLIB proxy) that overrides the annotated methods. Callers receive the proxy. A call from outside goes proxy → extra behavior → real method.
6. A call from one method of the real object to another method of **the same object** (`this.other()`) never touches the proxy. The extra behavior is skipped. This is the central trap of the second project.

## Walkthrough of this repository's use

```java
@Service
public class CheckoutService {

    private static final Logger LOG = LoggerFactory.getLogger(CheckoutService.class);

    private final CustomerService customers;
    private final OrderRepository orders;
    private final StockRepository stock;
    private final AuditLog audit;
    private final Clock clock;

    /**
     * Wires the service.
     *
     * @param customers customer lookup
     * @param orders where orders are kept
     * @param stock the units on hand
     * @param audit the audit trail, a separate bean so its own transaction can commit independently
     * @param clock the time source
     */
    public CheckoutService(
            CustomerService customers,
            OrderRepository orders,
            StockRepository stock,
            AuditLog audit,
            Clock clock) {
        this.customers = customers;
        this.orders = orders;
        this.stock = stock;
        this.audit = audit;
        this.clock = clock;
    }
```

`@Service` registers the bean. The constructor takes five collaborators, three of them interfaces from the domain (`OrderRepository`, `StockRepository`, `AuditLog`). Because `AuditLog` is an interface, the service neither knows nor cares that `JpaAuditLog` implements it, and a unit test passes a fake.

```java
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

    /**
     * Provides the system clock in UTC.
     *
     * @return the clock
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
```

`@Configuration(proxyBeanMethods = false)` marks a class that only declares beans, and tells Spring not to wrap it in a proxy (nothing here calls its own `@Bean` methods). `clock()` returns the system clock in UTC; tests replace this bean with a fixed clock so time is deterministic.

**The proxy rule, in code.** The architecture test makes the rule checkable:

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

`ArchitectureTest` also contains a rule that fails if a class calls a `@Transactional` method of its own class.

**Every production class that Spring creates as a bean** (marked `@Component`, `@Service`, `@Repository`, `@RestController`, `@RestControllerAdvice` or `@Configuration`):

| File | What it is |
| --- | --- |
| `src/main/java/dev/plainround/orders/application/AuditService.java` | Reads the audit trail. |
| `src/main/java/dev/plainround/orders/application/CheckoutService.java` | Places an order: all of it, or none of it. |
| `src/main/java/dev/plainround/orders/application/CustomerService.java` | Registers customers and looks them up. |
| `src/main/java/dev/plainround/orders/application/OrderQueryService.java` | Reads a customer's orders. |
| `src/main/java/dev/plainround/orders/application/StockService.java` | Sets and reads the units on hand. |
| `src/main/java/dev/plainround/orders/infrastructure/ClockConfig.java` | The one place the real clock is created; tests replace this bean with a fixed one. |
| `src/main/java/dev/plainround/orders/infrastructure/persistence/JpaAuditLog.java` | Adapter: the audit trail. |
| `src/main/java/dev/plainround/orders/infrastructure/persistence/JpaCustomerRepository.java` | Adapter: the customer port on top of Spring Data JPA. |
| `src/main/java/dev/plainround/orders/infrastructure/persistence/JpaOrderRepository.java` | Adapter: the order port on top of Spring Data JPA. |
| `src/main/java/dev/plainround/orders/infrastructure/persistence/JpaStockRepository.java` | Adapter: the stock port on top of Spring Data JPA, with optimistic locking on every change. |
| `src/main/java/dev/plainround/orders/interfaces/web/AuditController.java` | HTTP face of the audit trail (read only). |
| `src/main/java/dev/plainround/orders/interfaces/web/CustomerController.java` | HTTP face of the customers. |
| `src/main/java/dev/plainround/orders/interfaces/web/OrderController.java` | HTTP face of checkout and of a customer's order list. |
| `src/main/java/dev/plainround/orders/interfaces/web/OrdersExceptionHandler.java` | The one place errors become `application/problem+json`: the service's named errors, validation failures with the failing fields, the framework's own exceptions (through the parent class) and a last resort that never leaks the cause. |
| `src/main/java/dev/plainround/orders/interfaces/web/RequestIdFilter.java` | Gives every request an id for the logs: the caller's `X-Request-Id` when it looks safe, else a new one. |
| `src/main/java/dev/plainround/orders/interfaces/web/StockController.java` | HTTP face of the stock levels. |

## Try it yourself

1. Start the application and list the beans: read the startup log for `Bean 'checkoutService'` style lines with `--debug`, or add `beans` to `management.endpoints.web.exposure.include`.
2. Remove a bean annotation (for example `@Service` from one class) in a scratch copy and start: the error names the missing type.
3. Run `./mvnw test -Dtest=ArchitectureTest` to see the rules pass; add `@Autowired` to a field and run again to see one fail.

## Mistakes people make

- Calling `this.method()` and expecting an annotation on `method` to apply.
- Putting an annotation that needs a proxy on a `private` or `final` method: a subclass cannot override it.
- Creating a collaborator with `new` inside a class: it cannot be replaced in a test, and Spring cannot wrap it.
- Circular dependencies (A needs B, B needs A): constructor injection makes them fail at startup instead of at the first call.

## Check yourself

1. What is a bean? *An object Spring creates and keeps in the application context.*
2. Why does a class with one constructor need no `@Autowired`? *Spring uses the only constructor.*
3. What does a proxy add? *Behavior around a method call, such as a transaction.*
4. Why does `this.saveOrder()` skip a `@Transactional` on `saveOrder`? *The call does not go through the proxy.*
5. What does `@Bean` on a method do? *Registers the returned object as a bean.*

## Further reading

- Spring Framework 7.0.10 reference, "The IoC Container": https://docs.spring.io/spring-framework/reference/7.0.10/core/beans.html
- "Understanding AOP Proxies": https://docs.spring.io/spring-framework/reference/7.0.10/core/aop/proxying.html

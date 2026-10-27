# The application code, class by class

## What it is

This note is a tour of the Java code that makes up the running program (the code in `src/main/java`; the tests are in [the test code note](java-test-code.md)). Every class is printed in full, exactly as it is in the repository, and under each one is a reading guide that explains each field, constructor and method in plain words.

If you have never seen Java, read [Java](java.md) first. It explains packages, classes, methods, imports and types. This note then shows all of them in use.

## Why this repository uses it

The code is the product. The repository is small on purpose so that you can read all of it. Reading every class, in the order given below, shows how a web request turns into a result.

## Where it appears

All files are under `src/main/java/dev/plainround/orders/`. The folders are layers:

| Folder | Layer | Job |
| --- | --- | --- |
| `domain` | the business rules | plain Java with no framework |
| `application` | the use cases | services that run a checkout, list orders, count stock |
| `infrastructure/persistence` | the database details | JPA entities, Spring Data repositories and the classes that hide them |
| `interfaces/web` | the HTTP face | controllers and error mapping |

## How it works underneath

1. When the program starts, Java runs the `main` method of the application class. That method calls `SpringApplication.run`, which scans the package and its sub-packages for classes carrying Spring annotations and creates one object of each.
2. Objects ask for the other objects they need in their constructors. Spring finds a matching object and passes it in. This is called constructor injection.
3. When a request arrives, Tomcat (the web server inside the program) hands it to Spring, which picks the controller method by URL and HTTP verb, converts JSON into Java objects, validates them, and calls the method.
4. The return value is converted back to JSON and written as the response. A thrown exception is turned into an error response by the exception handler class.

## Walkthrough of this repository's use

Each class below is printed whole. Under it, a list explains each part. The "What the body does" lines are a mechanical translation of the statements, in order, into words; they quote the code in backticks so you can match each line with the listing.

### `src/main/java/dev/plainround/orders/OrderServiceApplication.java`

Package `dev.plainround.orders`. Entry point of the order service.

The whole file, as it is in the repository:

```java
package dev.plainround.orders;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point of the order service. */
@SpringBootApplication
public class OrderServiceApplication {

    /**
     * Starts the application.
     *
     * @param args command-line arguments, passed to Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
```

Reading it, one piece at a time:

- **`OrderServiceApplication`**, a class. Entry point of the order service.
  - annotation `@SpringBootApplication`: marks the starting class: it turns on auto-configuration and scans this package and below for components
  - **method `main`**: Starts the application.
    - Signature: `public static void main(String[] args)`
    - input `args`: command-line arguments, passed to Spring Boot
    - What the body does, in order:
      - calls `SpringApplication.run(OrderServiceApplication.class, args)`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@SpringBootApplication`: marks the starting class: it turns on auto-configuration and scans this package and below for components.

Language features in this file, explained:

- **static**: `static` means the member belongs to the class itself, not to one object; you use it without creating an object.
- **public**: `public` lets any class use the member.


### `src/main/java/dev/plainround/orders/application/AuditService.java`

Package `dev.plainround.orders.application`. Reads the audit trail.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.application;

import dev.plainround.orders.domain.AuditEntry;
import dev.plainround.orders.domain.AuditLog;
import java.util.List;
import org.springframework.stereotype.Service;

/** Reads the audit trail. */
@Service
public class AuditService {

    private final AuditLog audit;

    /**
     * Wires the service.
     *
     * @param audit the audit trail
     */
    public AuditService(AuditLog audit) {
        this.audit = audit;
    }

    /**
     * Returns the newest entries first.
     *
     * @param limit how many at most
     * @return the entries
     */
    public List<AuditEntry> latest(int limit) {
        return audit.latest(limit);
    }
}
```

Reading it, one piece at a time:

- **`AuditService`**, a class. Reads the audit trail.
  - annotation `@Service`: tells Spring to create one shared instance of this business-logic class at startup
  - **field `audit`** of type `AuditLog`, set once when the object is built.
    - Declaration: `private final AuditLog audit`
  - **method `AuditService`**: Wires the service.
    - Signature: `public AuditService(AuditLog audit)`
    - input `audit`: the audit trail
    - What the body does, in order:
      - stores `audit` in the field `audit`
  - **method `latest`**: Returns the newest entries first.
    - Signature: `public List<AuditEntry> latest(int limit)`
    - input `limit`: how many at most
    - gives back: the entries
    - What the body does, in order:
      - returns `audit.latest(limit)`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@Service`: tells Spring to create one shared instance of this business-logic class at startup.

Language features in this file, explained:

- **final**: `final` on a field or variable means it is assigned once and never changed; on a class it forbids subclasses.
- **private**: `private` hides a member from every other class.
- **public**: `public` lets any class use the member.
- **List**: A `List` is an ordered collection of items; `List.of(...)` builds one that cannot be changed afterwards.


### `src/main/java/dev/plainround/orders/application/CheckoutService.java`

Package `dev.plainround.orders.application`. Places an order: all of it, or none of it.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.application;

import dev.plainround.orders.domain.AuditLog;
import dev.plainround.orders.domain.Order;
import dev.plainround.orders.domain.OrderDraft;
import dev.plainround.orders.domain.OrderLine;
import dev.plainround.orders.domain.OrderRepository;
import dev.plainround.orders.domain.StockRepository;
import java.time.Clock;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Places an order: all of it, or none of it. */
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

    /**
     * Checks out. The transaction starts here, on the public entry point, so every step below is
     * inside one unit of work: if a later line is out of stock, the order and the earlier
     * reservations roll back together. The audit entry is written first through another bean and
     * survives that rollback.
     *
     * @param customerId the buyer
     * @param lines what is being bought
     * @return the stored order
     */
    @Transactional
    public Order checkout(long customerId, List<OrderLine> lines) {
        audit.record(
                "CHECKOUT_ATTEMPT", "customer " + customerId + ", " + lines.size() + " line(s)");
        customers.require(customerId);
        Order placed = orders.place(new OrderDraft(customerId, lines), clock.instant());
        lines.forEach(line -> stock.reserve(line.sku(), line.quantity()));
        LOG.info("order placed id={} customerId={}", placed.id(), customerId);
        return placed;
    }
}
```

Reading it, one piece at a time:

- **`CheckoutService`**, a class. Places an order: all of it, or none of it.
  - annotation `@Service`: tells Spring to create one shared instance of this business-logic class at startup
  - **field `LOG`** of type `Logger`, starting as `LoggerFactory.getLogger(CheckoutService.class)`
    - Declaration: `private static final Logger LOG = LoggerFactory.getLogger(CheckoutService.class)`
  - **field `customers`** of type `CustomerService`, set once when the object is built.
    - Declaration: `private final CustomerService customers`
  - **field `orders`** of type `OrderRepository`, set once when the object is built.
    - Declaration: `private final OrderRepository orders`
  - **field `stock`** of type `StockRepository`, set once when the object is built.
    - Declaration: `private final StockRepository stock`
  - **field `audit`** of type `AuditLog`, set once when the object is built.
    - Declaration: `private final AuditLog audit`
  - **field `clock`** of type `Clock`, set once when the object is built.
    - Declaration: `private final Clock clock`
  - **method `CheckoutService`**: Wires the service.
    - Signature: `public CheckoutService( CustomerService customers, OrderRepository orders, StockRepository stock, AuditLog audit, Clock clock)`
    - input `customers`: customer lookup
    - input `orders`: where orders are kept
    - input `stock`: the units on hand
    - input `audit`: the audit trail, a separate bean so its own transaction can commit independently
    - input `clock`: the time source
    - What the body does, in order:
      - stores `customers` in the field `customers`
      - stores `orders` in the field `orders`
      - stores `stock` in the field `stock`
      - stores `audit` in the field `audit`
      - stores `clock` in the field `clock`
  - **method `checkout`**: Checks out. The transaction starts here, on the public entry point, so every step below is inside one unit of work: if a later line is out of stock, the order and the earlier reservations roll back together. The audit entry is written first through another bean and survives that rollback.
    - Signature: `public Order checkout(long customerId, List<OrderLine> lines)`
    - input `customerId`: the buyer
    - input `lines`: what is being bought
    - gives back: the stored order
    - annotation `@Transactional`: wraps the call in a database transaction (a group of changes that all succeed or all roll back)
    - What the body does, in order:
      - calls `audit.record( "CHECKOUT_ATTEMPT", "customer " + customerId + ", " + lines.size() + " line(s)")`
      - calls `customers.require(customerId)`
      - creates the variable `placed` (of type `Order`) holding `orders.place(new OrderDraft(customerId, lines), clock.instant())`
      - calls `lines.forEach(line -> stock.reserve(line.sku(), line.quantity()))`
      - calls `LOG.info("order placed id={} customerId={}", placed.id(), customerId)`
      - returns `placed`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@Service`: tells Spring to create one shared instance of this business-logic class at startup.
- `@Transactional`: wraps the call in a database transaction (a group of changes that all succeed or all roll back).

Language features in this file, explained:

- **final**: `final` on a field or variable means it is assigned once and never changed; on a class it forbids subclasses.
- **static**: `static` means the member belongs to the class itself, not to one object; you use it without creating an object.
- **private**: `private` hides a member from every other class.
- **public**: `public` lets any class use the member.
- **lambda (`->`)**: An arrow `a -> b` is a lambda: a small unnamed function. The part before the arrow lists its inputs and the part after is what it does.
- **List**: A `List` is an ordered collection of items; `List.of(...)` builds one that cannot be changed afterwards.


### `src/main/java/dev/plainround/orders/application/CustomerService.java`

Package `dev.plainround.orders.application`. Registers customers and looks them up.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.application;

import dev.plainround.orders.domain.Customer;
import dev.plainround.orders.domain.CustomerNotFound;
import dev.plainround.orders.domain.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registers customers and looks them up. */
@Service
public class CustomerService {

    private final CustomerRepository customers;

    /**
     * Wires the service.
     *
     * @param customers where customers are kept
     */
    public CustomerService(CustomerRepository customers) {
        this.customers = customers;
    }

    /**
     * Registers a customer.
     *
     * @param name the display name
     * @return the stored customer
     */
    @Transactional
    public Customer register(String name) {
        Customer.requireName(name);
        return customers.register(name);
    }

    /**
     * Returns a customer or fails with a named error.
     *
     * @param id the customer id
     * @return the customer
     * @throws CustomerNotFound if there is none
     */
    @Transactional(readOnly = true)
    public Customer require(long id) {
        return customers.findById(id).orElseThrow(() -> new CustomerNotFound(id));
    }
}
```

Reading it, one piece at a time:

- **`CustomerService`**, a class. Registers customers and looks them up.
  - annotation `@Service`: tells Spring to create one shared instance of this business-logic class at startup
  - **field `customers`** of type `CustomerRepository`, set once when the object is built.
    - Declaration: `private final CustomerRepository customers`
  - **method `CustomerService`**: Wires the service.
    - Signature: `public CustomerService(CustomerRepository customers)`
    - input `customers`: where customers are kept
    - What the body does, in order:
      - stores `customers` in the field `customers`
  - **method `register`**: Registers a customer.
    - Signature: `public Customer register(String name)`
    - input `name`: the display name
    - gives back: the stored customer
    - annotation `@Transactional`: wraps the call in a database transaction (a group of changes that all succeed or all roll back)
    - What the body does, in order:
      - calls `Customer.requireName(name)`
      - returns `customers.register(name)`
  - **method `require`**: Returns a customer or fails with a named error.
    - Signature: `public Customer require(long id)`
    - input `id`: the customer id
    - gives back: the customer
    - throws `CustomerNotFound`: if there is none
    - annotation `@Transactional(readOnly = true)`: wraps the call in a database transaction (a group of changes that all succeed or all roll back)
    - What the body does, in order:
      - returns `customers.findById(id).orElseThrow(() -> new CustomerNotFound(id))`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@Service`: tells Spring to create one shared instance of this business-logic class at startup.
- `@Transactional`: wraps the call in a database transaction (a group of changes that all succeed or all roll back).

Language features in this file, explained:

- **final**: `final` on a field or variable means it is assigned once and never changed; on a class it forbids subclasses.
- **private**: `private` hides a member from every other class.
- **public**: `public` lets any class use the member.
- **lambda (`->`)**: An arrow `a -> b` is a lambda: a small unnamed function. The part before the arrow lists its inputs and the part after is what it does.


### `src/main/java/dev/plainround/orders/application/OrderQueryService.java`

Package `dev.plainround.orders.application`. Reads a customer's orders.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.application;

import dev.plainround.orders.domain.Order;
import dev.plainround.orders.domain.OrderRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads a customer's orders. */
@Service
public class OrderQueryService {

    private final CustomerService customers;
    private final OrderRepository orders;

    /**
     * Wires the service.
     *
     * @param customers customer lookup
     * @param orders where orders are kept
     */
    public OrderQueryService(CustomerService customers, OrderRepository orders) {
        this.customers = customers;
        this.orders = orders;
    }

    /**
     * Lists a customer's orders with their lines.
     *
     * @param customerId the buyer
     * @return the orders, oldest first
     */
    @Transactional(readOnly = true)
    public List<Order> listOrders(long customerId) {
        customers.require(customerId);
        return orders.findByCustomerId(customerId);
    }
}
```

Reading it, one piece at a time:

- **`OrderQueryService`**, a class. Reads a customer's orders.
  - annotation `@Service`: tells Spring to create one shared instance of this business-logic class at startup
  - **field `customers`** of type `CustomerService`, set once when the object is built.
    - Declaration: `private final CustomerService customers`
  - **field `orders`** of type `OrderRepository`, set once when the object is built.
    - Declaration: `private final OrderRepository orders`
  - **method `OrderQueryService`**: Wires the service.
    - Signature: `public OrderQueryService(CustomerService customers, OrderRepository orders)`
    - input `customers`: customer lookup
    - input `orders`: where orders are kept
    - What the body does, in order:
      - stores `customers` in the field `customers`
      - stores `orders` in the field `orders`
  - **method `listOrders`**: Lists a customer's orders with their lines.
    - Signature: `public List<Order> listOrders(long customerId)`
    - input `customerId`: the buyer
    - gives back: the orders, oldest first
    - annotation `@Transactional(readOnly = true)`: wraps the call in a database transaction (a group of changes that all succeed or all roll back)
    - What the body does, in order:
      - calls `customers.require(customerId)`
      - returns `orders.findByCustomerId(customerId)`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@Service`: tells Spring to create one shared instance of this business-logic class at startup.
- `@Transactional`: wraps the call in a database transaction (a group of changes that all succeed or all roll back).

Language features in this file, explained:

- **final**: `final` on a field or variable means it is assigned once and never changed; on a class it forbids subclasses.
- **private**: `private` hides a member from every other class.
- **public**: `public` lets any class use the member.
- **List**: A `List` is an ordered collection of items; `List.of(...)` builds one that cannot be changed afterwards.


### `src/main/java/dev/plainround/orders/application/StockService.java`

Package `dev.plainround.orders.application`. Sets and reads the units on hand.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.application;

import dev.plainround.orders.domain.OutOfStock;
import dev.plainround.orders.domain.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sets and reads the units on hand. */
@Service
public class StockService {

    private final StockRepository stock;

    /**
     * Wires the service.
     *
     * @param stock the units on hand
     */
    public StockService(StockRepository stock) {
        this.stock = stock;
    }

    /**
     * Sets the units on hand.
     *
     * @param sku the product code
     * @param available the units
     */
    @Transactional
    public void setAvailable(String sku, int available) {
        stock.setAvailable(sku, available);
    }

    /**
     * Reports the units on hand.
     *
     * @param sku the product code
     * @return the units
     * @throws OutOfStock if the SKU has no stock record
     */
    @Transactional(readOnly = true)
    public int available(String sku) {
        return stock.available(sku).orElseThrow(() -> new OutOfStock(sku));
    }
}
```

Reading it, one piece at a time:

- **`StockService`**, a class. Sets and reads the units on hand.
  - annotation `@Service`: tells Spring to create one shared instance of this business-logic class at startup
  - **field `stock`** of type `StockRepository`, set once when the object is built.
    - Declaration: `private final StockRepository stock`
  - **method `StockService`**: Wires the service.
    - Signature: `public StockService(StockRepository stock)`
    - input `stock`: the units on hand
    - What the body does, in order:
      - stores `stock` in the field `stock`
  - **method `setAvailable`**: Sets the units on hand.
    - Signature: `public void setAvailable(String sku, int available)`
    - input `sku`: the product code
    - input `available`: the units
    - annotation `@Transactional`: wraps the call in a database transaction (a group of changes that all succeed or all roll back)
    - What the body does, in order:
      - calls `stock.setAvailable(sku, available)`
  - **method `available`**: Reports the units on hand.
    - Signature: `public int available(String sku)`
    - input `sku`: the product code
    - gives back: the units
    - throws `OutOfStock`: if the SKU has no stock record
    - annotation `@Transactional(readOnly = true)`: wraps the call in a database transaction (a group of changes that all succeed or all roll back)
    - What the body does, in order:
      - returns `stock.available(sku).orElseThrow(() -> new OutOfStock(sku))`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@Service`: tells Spring to create one shared instance of this business-logic class at startup.
- `@Transactional`: wraps the call in a database transaction (a group of changes that all succeed or all roll back).

Language features in this file, explained:

- **final**: `final` on a field or variable means it is assigned once and never changed; on a class it forbids subclasses.
- **private**: `private` hides a member from every other class.
- **public**: `public` lets any class use the member.
- **lambda (`->`)**: An arrow `a -> b` is a lambda: a small unnamed function. The part before the arrow lists its inputs and the part after is what it does.


### `src/main/java/dev/plainround/orders/domain/AuditEntry.java`

Package `dev.plainround.orders.domain`. One line of the audit trail.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.domain;

import java.time.Instant;

/**
 * One line of the audit trail.
 *
 * @param id the identifier assigned by the store
 * @param action what was attempted, such as {@code CHECKOUT_ATTEMPT}
 * @param detail free text that says who and what, never a secret
 * @param recordedAt when the entry was written
 */
public record AuditEntry(long id, String action, String detail, Instant recordedAt) {}
```

Reading it, one piece at a time:

- **`AuditEntry`**, a record carrying `long id, String action, String detail, Instant recordedAt`. One line of the audit trail.

Language features in this file, explained:

- **record**: A `record` is a short way to declare a class that only carries data. The names in the parentheses become fields, a constructor, read methods with the same names, and `equals`, `hashCode` and `toString`.
- **public**: `public` lets any class use the member.


### `src/main/java/dev/plainround/orders/domain/AuditLog.java`

Package `dev.plainround.orders.domain`. Port: the audit trail.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.domain;

import java.util.List;

/** Port: the audit trail. Writing to it must survive the rollback of the transaction that asked. */
public interface AuditLog {

    /**
     * Writes one entry and commits it on its own, whatever happens to the caller's transaction.
     *
     * @param action what was attempted
     * @param detail who and what
     */
    void record(String action, String detail);

    /**
     * Returns the newest entries first.
     *
     * @param limit how many entries at most
     * @return the entries
     */
    List<AuditEntry> latest(int limit);
}
```

Reading it, one piece at a time:

- **`AuditLog`**, a interface. Port: the audit trail. Writing to it must survive the rollback of the transaction that asked.
  - **method `record`**: Writes one entry and commits it on its own, whatever happens to the caller's transaction.
    - Signature: `void record(String action, String detail)`
    - input `action`: what was attempted
    - input `detail`: who and what
  - **method `latest`**: Returns the newest entries first.
    - Signature: `List<AuditEntry> latest(int limit)`
    - input `limit`: how many entries at most
    - gives back: the entries

Language features in this file, explained:

- **interface**: An `interface` lists method signatures without bodies. Any class that says it `implements` the interface must provide those methods, so callers can depend on the list alone.
- **public**: `public` lets any class use the member.
- **List**: A `List` is an ordered collection of items; `List.of(...)` builds one that cannot be changed afterwards.


### `src/main/java/dev/plainround/orders/domain/Customer.java`

Package `dev.plainround.orders.domain`. A customer who places orders.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.domain;

/**
 * A customer who places orders.
 *
 * @param id the identifier assigned by the store
 * @param name the display name, 1 to {@value #MAX_NAME_LENGTH} characters, not blank
 */
public record Customer(long id, String name) {

    /** Longest accepted name. */
    public static final int MAX_NAME_LENGTH = 100;

    /** Checks the invariants. */
    public Customer {
        requireName(name);
    }

    /**
     * Checks a name before the customer is stored, when there is no id yet.
     *
     * @param name the candidate name
     * @throws InvalidCustomer if the name is blank or too long
     */
    public static void requireName(String name) {
        if (name == null || name.isBlank() || name.length() > MAX_NAME_LENGTH) {
            throw new InvalidCustomer("name must be 1 to " + MAX_NAME_LENGTH + " characters");
        }
    }
}
```

Reading it, one piece at a time:

- **`Customer`**, a record carrying `long id, String name`. A customer who places orders.
  - **field `MAX_NAME_LENGTH`** of type `int`: Longest accepted name.
    - Declaration: `public static final int MAX_NAME_LENGTH = 100`
  - **compact constructor (runs when the record is built, before the fields are set) `Customer`**: Checks the invariants.
    - Signature: `public Customer`
    - What the body does, in order:
      - calls `requireName(name)`
  - **method `requireName`**: Checks a name before the customer is stored, when there is no id yet.
    - Signature: `public static void requireName(String name)`
    - input `name`: the candidate name
    - throws `InvalidCustomer`: if the name is blank or too long
    - What the body does, in order:
      - when `name == null || name.isBlank() || name.length() > MAX_NAME_LENGTH` is true:
        - throws a new `InvalidCustomer` created with `"name must be 1 to " + MAX_NAME_LENGTH + " characters"`

Language features in this file, explained:

- **record**: A `record` is a short way to declare a class that only carries data. The names in the parentheses become fields, a constructor, read methods with the same names, and `equals`, `hashCode` and `toString`.
- **final**: `final` on a field or variable means it is assigned once and never changed; on a class it forbids subclasses.
- **static**: `static` means the member belongs to the class itself, not to one object; you use it without creating an object.
- **public**: `public` lets any class use the member.
- **null**: `null` means "no object". Calling a method on null throws `NullPointerException`.


### `src/main/java/dev/plainround/orders/domain/CustomerNotFound.java`

Package `dev.plainround.orders.domain`. No customer has the requested id.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.domain;

/** No customer has the requested id. */
public class CustomerNotFound extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the error.
     *
     * @param id the id that matched nothing
     */
    public CustomerNotFound(long id) {
        super("no customer with id " + id);
    }
}
```

Reading it, one piece at a time:

- **`CustomerNotFound`**, a class that extends `RuntimeException`. No customer has the requested id.
  - **field `serialVersionUID`** of type `long`, starting as `1L`
    - Declaration: `private static final long serialVersionUID = 1L`
  - **method `CustomerNotFound`**: Creates the error.
    - Signature: `public CustomerNotFound(long id)`
    - input `id`: the id that matched nothing
    - What the body does, in order:
      - calls the parent constructor: `super("no customer with id " + id)`

Language features in this file, explained:

- **final**: `final` on a field or variable means it is assigned once and never changed; on a class it forbids subclasses.
- **static**: `static` means the member belongs to the class itself, not to one object; you use it without creating an object.
- **private**: `private` hides a member from every other class.
- **public**: `public` lets any class use the member.
- **extends**: `extends X` makes the class a child of `X`: it inherits what `X` has and may replace parts.


### `src/main/java/dev/plainround/orders/domain/CustomerRepository.java`

Package `dev.plainround.orders.domain`. Port: where customers are kept.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.domain;

import java.util.Optional;

/** Port: where customers are kept. */
public interface CustomerRepository {

    /**
     * Stores a new customer.
     *
     * @param name the validated display name
     * @return the stored customer with its id
     */
    Customer register(String name);

    /**
     * Looks a customer up.
     *
     * @param id the customer id
     * @return the customer, or empty if there is none
     */
    Optional<Customer> findById(long id);
}
```

Reading it, one piece at a time:

- **`CustomerRepository`**, a interface. Port: where customers are kept.
  - **method `register`**: Stores a new customer.
    - Signature: `Customer register(String name)`
    - input `name`: the validated display name
    - gives back: the stored customer with its id
  - **method `findById`**: Looks a customer up.
    - Signature: `Optional<Customer> findById(long id)`
    - input `id`: the customer id
    - gives back: the customer, or empty if there is none

Language features in this file, explained:

- **interface**: An `interface` lists method signatures without bodies. Any class that says it `implements` the interface must provide those methods, so callers can depend on the list alone.
- **public**: `public` lets any class use the member.
- **Optional**: `Optional<T>` is a box that holds either one `T` or nothing. It makes "may be missing" visible in the type instead of using null.


### `src/main/java/dev/plainround/orders/domain/InvalidCustomer.java`

Package `dev.plainround.orders.domain`. A customer was built with values that break its invariants.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.domain;

/** A customer was built with values that break its invariants. */
public class InvalidCustomer extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the error.
     *
     * @param message which rule was broken
     */
    public InvalidCustomer(String message) {
        super(message);
    }
}
```

Reading it, one piece at a time:

- **`InvalidCustomer`**, a class that extends `RuntimeException`. A customer was built with values that break its invariants.
  - **field `serialVersionUID`** of type `long`, starting as `1L`
    - Declaration: `private static final long serialVersionUID = 1L`
  - **method `InvalidCustomer`**: Creates the error.
    - Signature: `public InvalidCustomer(String message)`
    - input `message`: which rule was broken
    - What the body does, in order:
      - calls the parent constructor: `super(message)`

Language features in this file, explained:

- **final**: `final` on a field or variable means it is assigned once and never changed; on a class it forbids subclasses.
- **static**: `static` means the member belongs to the class itself, not to one object; you use it without creating an object.
- **private**: `private` hides a member from every other class.
- **public**: `public` lets any class use the member.
- **extends**: `extends X` makes the class a child of `X`: it inherits what `X` has and may replace parts.


### `src/main/java/dev/plainround/orders/domain/InvalidOrder.java`

Package `dev.plainround.orders.domain`. An order or one of its lines breaks a rule, such as an empty basket or the same SKU twice.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.domain;

/** An order or one of its lines breaks a rule, such as an empty basket or the same SKU twice. */
public class InvalidOrder extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the error.
     *
     * @param message which rule was broken
     */
    public InvalidOrder(String message) {
        super(message);
    }
}
```

Reading it, one piece at a time:

- **`InvalidOrder`**, a class that extends `RuntimeException`. An order or one of its lines breaks a rule, such as an empty basket or the same SKU twice.
  - **field `serialVersionUID`** of type `long`, starting as `1L`
    - Declaration: `private static final long serialVersionUID = 1L`
  - **method `InvalidOrder`**: Creates the error.
    - Signature: `public InvalidOrder(String message)`
    - input `message`: which rule was broken
    - What the body does, in order:
      - calls the parent constructor: `super(message)`

Language features in this file, explained:

- **final**: `final` on a field or variable means it is assigned once and never changed; on a class it forbids subclasses.
- **static**: `static` means the member belongs to the class itself, not to one object; you use it without creating an object.
- **private**: `private` hides a member from every other class.
- **public**: `public` lets any class use the member.
- **extends**: `extends X` makes the class a child of `X`: it inherits what `X` has and may replace parts.


### `src/main/java/dev/plainround/orders/domain/Order.java`

Package `dev.plainround.orders.domain`. A stored order with its lines.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.domain;

import java.time.Instant;
import java.util.List;

/**
 * A stored order with its lines.
 *
 * @param id the identifier assigned by the store
 * @param customerId the buyer
 * @param placedAt when the order was placed
 * @param lines the lines, in the order they were given
 */
public record Order(long id, long customerId, Instant placedAt, List<OrderLine> lines) {

    /** Keeps an immutable copy of the lines. */
    public Order {
        lines = List.copyOf(lines);
    }
}
```

Reading it, one piece at a time:

- **`Order`**, a record carrying `long id, long customerId, Instant placedAt, List<OrderLine> lines`. A stored order with its lines.
  - **compact constructor (runs when the record is built, before the fields are set) `Order`**: Keeps an immutable copy of the lines.
    - Signature: `public Order`
    - What the body does, in order:
      - sets `lines` to `List.copyOf(lines)`

Language features in this file, explained:

- **record**: A `record` is a short way to declare a class that only carries data. The names in the parentheses become fields, a constructor, read methods with the same names, and `equals`, `hashCode` and `toString`.
- **public**: `public` lets any class use the member.
- **List**: A `List` is an ordered collection of items; `List.of(...)` builds one that cannot be changed afterwards.


### `src/main/java/dev/plainround/orders/domain/OrderDraft.java`

Package `dev.plainround.orders.domain`. What a customer asks to buy, before it is stored.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.domain;

import java.util.HashSet;
import java.util.List;

/**
 * What a customer asks to buy, before it is stored.
 *
 * @param customerId the buyer
 * @param lines 1 to {@value #MAX_LINES} lines with distinct SKUs
 */
public record OrderDraft(long customerId, List<OrderLine> lines) {

    /** Most lines in one order. */
    public static final int MAX_LINES = 50;

    /** Checks the invariants and keeps an immutable copy of the lines. */
    public OrderDraft {
        lines = List.copyOf(lines);
        if (lines.isEmpty() || lines.size() > MAX_LINES) {
            throw new InvalidOrder("an order needs 1 to " + MAX_LINES + " lines");
        }
        if (new HashSet<>(lines.stream().map(OrderLine::sku).toList()).size() != lines.size()) {
            throw new InvalidOrder("each sku may appear on one line only");
        }
    }
}
```

Reading it, one piece at a time:

- **`OrderDraft`**, a record carrying `long customerId, List<OrderLine> lines`. What a customer asks to buy, before it is stored.
  - **field `MAX_LINES`** of type `int`: Most lines in one order.
    - Declaration: `public static final int MAX_LINES = 50`
  - **compact constructor (runs when the record is built, before the fields are set) `OrderDraft`**: Checks the invariants and keeps an immutable copy of the lines.
    - Signature: `public OrderDraft`
    - What the body does, in order:
      - sets `lines` to `List.copyOf(lines)`
      - when `lines.isEmpty() || lines.size() > MAX_LINES` is true:
        - throws a new `InvalidOrder` created with `"an order needs 1 to " + MAX_LINES + " lines"`
      - when `new HashSet<>(lines.stream().map(OrderLine::sku).toList()).size() != lines.size()` is true:
        - throws a new `InvalidOrder` created with `"each sku may appear on one line only"`

Language features in this file, explained:

- **record**: A `record` is a short way to declare a class that only carries data. The names in the parentheses become fields, a constructor, read methods with the same names, and `equals`, `hashCode` and `toString`.
- **final**: `final` on a field or variable means it is assigned once and never changed; on a class it forbids subclasses.
- **static**: `static` means the member belongs to the class itself, not to one object; you use it without creating an object.
- **public**: `public` lets any class use the member.
- **method reference (`::`)**: `Type::method` is a short way to pass an existing method where a function is expected.
- **List**: A `List` is an ordered collection of items; `List.of(...)` builds one that cannot be changed afterwards.
- **Stream**: A stream processes a collection step by step: `map` converts each item, `filter` keeps some, `toList` collects the result.


### `src/main/java/dev/plainround/orders/domain/OrderLine.java`

Package `dev.plainround.orders.domain`. One line of an order.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.domain;

/**
 * One line of an order.
 *
 * @param sku the product code, 1 to {@value #MAX_SKU_LENGTH} characters, not blank
 * @param quantity the number of units, 1 to {@value #MAX_QUANTITY}
 */
public record OrderLine(String sku, int quantity) {

    /** Longest accepted SKU. */
    public static final int MAX_SKU_LENGTH = 64;

    /** Largest quantity on one line. */
    public static final int MAX_QUANTITY = 1000;

    /** Checks the invariants. */
    public OrderLine {
        if (sku == null || sku.isBlank() || sku.length() > MAX_SKU_LENGTH) {
            throw new InvalidOrder("sku must be 1 to " + MAX_SKU_LENGTH + " characters");
        }
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            throw new InvalidOrder("quantity must be between 1 and " + MAX_QUANTITY);
        }
    }
}
```

Reading it, one piece at a time:

- **`OrderLine`**, a record carrying `String sku, int quantity`. One line of an order.
  - **field `MAX_SKU_LENGTH`** of type `int`: Longest accepted SKU.
    - Declaration: `public static final int MAX_SKU_LENGTH = 64`
  - **field `MAX_QUANTITY`** of type `int`: Largest quantity on one line.
    - Declaration: `public static final int MAX_QUANTITY = 1000`
  - **compact constructor (runs when the record is built, before the fields are set) `OrderLine`**: Checks the invariants.
    - Signature: `public OrderLine`
    - What the body does, in order:
      - when `sku == null || sku.isBlank() || sku.length() > MAX_SKU_LENGTH` is true:
        - throws a new `InvalidOrder` created with `"sku must be 1 to " + MAX_SKU_LENGTH + " characters"`
      - when `quantity < 1 || quantity > MAX_QUANTITY` is true:
        - throws a new `InvalidOrder` created with `"quantity must be between 1 and " + MAX_QUANTITY`

Language features in this file, explained:

- **record**: A `record` is a short way to declare a class that only carries data. The names in the parentheses become fields, a constructor, read methods with the same names, and `equals`, `hashCode` and `toString`.
- **final**: `final` on a field or variable means it is assigned once and never changed; on a class it forbids subclasses.
- **static**: `static` means the member belongs to the class itself, not to one object; you use it without creating an object.
- **public**: `public` lets any class use the member.
- **null**: `null` means "no object". Calling a method on null throws `NullPointerException`.


### `src/main/java/dev/plainround/orders/domain/OrderRepository.java`

Package `dev.plainround.orders.domain`. Port: where orders are kept.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.domain;

import java.time.Instant;
import java.util.List;

/** Port: where orders are kept. */
public interface OrderRepository {

    /**
     * Stores an order with all its lines.
     *
     * @param draft what is being bought
     * @param placedAt when the order is placed
     * @return the stored order with its id
     */
    Order place(OrderDraft draft, Instant placedAt);

    /**
     * Lists a customer's orders, oldest first, with their lines, in a number of queries that does
     * not depend on how many orders there are.
     *
     * @param customerId the buyer
     * @return the orders, empty if there are none
     */
    List<Order> findByCustomerId(long customerId);
}
```

Reading it, one piece at a time:

- **`OrderRepository`**, a interface. Port: where orders are kept.
  - **method `place`**: Stores an order with all its lines.
    - Signature: `Order place(OrderDraft draft, Instant placedAt)`
    - input `draft`: what is being bought
    - input `placedAt`: when the order is placed
    - gives back: the stored order with its id
  - **method `findByCustomerId`**: Lists a customer's orders, oldest first, with their lines, in a number of queries that does not depend on how many orders there are.
    - Signature: `List<Order> findByCustomerId(long customerId)`
    - input `customerId`: the buyer
    - gives back: the orders, empty if there are none

Language features in this file, explained:

- **interface**: An `interface` lists method signatures without bodies. Any class that says it `implements` the interface must provide those methods, so callers can depend on the list alone.
- **public**: `public` lets any class use the member.
- **List**: A `List` is an ordered collection of items; `List.of(...)` builds one that cannot be changed afterwards.


### `src/main/java/dev/plainround/orders/domain/OutOfStock.java`

Package `dev.plainround.orders.domain`. A SKU has too few units left, or no stock record at all.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.domain;

/** A SKU has too few units left, or no stock record at all. */
public class OutOfStock extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the error.
     *
     * @param sku the SKU that cannot cover the quantity
     */
    public OutOfStock(String sku) {
        super("not enough stock for " + sku);
    }
}
```

Reading it, one piece at a time:

- **`OutOfStock`**, a class that extends `RuntimeException`. A SKU has too few units left, or no stock record at all.
  - **field `serialVersionUID`** of type `long`, starting as `1L`
    - Declaration: `private static final long serialVersionUID = 1L`
  - **method `OutOfStock`**: Creates the error.
    - Signature: `public OutOfStock(String sku)`
    - input `sku`: the SKU that cannot cover the quantity
    - What the body does, in order:
      - calls the parent constructor: `super("not enough stock for " + sku)`

Language features in this file, explained:

- **final**: `final` on a field or variable means it is assigned once and never changed; on a class it forbids subclasses.
- **static**: `static` means the member belongs to the class itself, not to one object; you use it without creating an object.
- **private**: `private` hides a member from every other class.
- **public**: `public` lets any class use the member.
- **extends**: `extends X` makes the class a child of `X`: it inherits what `X` has and may replace parts.


### `src/main/java/dev/plainround/orders/domain/StockConflict.java`

Package `dev.plainround.orders.domain`. Another checkout changed the same stock row first; the caller may retry the whole checkout.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.domain;

/** Another checkout changed the same stock row first; the caller may retry the whole checkout. */
public class StockConflict extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the error.
     *
     * @param sku the SKU whose stock row changed underneath this checkout
     */
    public StockConflict(String sku) {
        super("stock for " + sku + " was changed by another checkout");
    }
}
```

Reading it, one piece at a time:

- **`StockConflict`**, a class that extends `RuntimeException`. Another checkout changed the same stock row first; the caller may retry the whole checkout.
  - **field `serialVersionUID`** of type `long`, starting as `1L`
    - Declaration: `private static final long serialVersionUID = 1L`
  - **method `StockConflict`**: Creates the error.
    - Signature: `public StockConflict(String sku)`
    - input `sku`: the SKU whose stock row changed underneath this checkout
    - What the body does, in order:
      - calls the parent constructor: `super("stock for " + sku + " was changed by another checkout")`

Language features in this file, explained:

- **final**: `final` on a field or variable means it is assigned once and never changed; on a class it forbids subclasses.
- **static**: `static` means the member belongs to the class itself, not to one object; you use it without creating an object.
- **private**: `private` hides a member from every other class.
- **public**: `public` lets any class use the member.
- **extends**: `extends X` makes the class a child of `X`: it inherits what `X` has and may replace parts.


### `src/main/java/dev/plainround/orders/domain/StockRepository.java`

Package `dev.plainround.orders.domain`. Port: the units on hand per SKU.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.domain;

import java.util.Optional;

/** Port: the units on hand per SKU. */
public interface StockRepository {

    /**
     * Sets the units on hand, creating the record if the SKU is new.
     *
     * @param sku the product code
     * @param available the units on hand
     */
    void setAvailable(String sku, int available);

    /**
     * Reports the units on hand.
     *
     * @param sku the product code
     * @return the units, or empty if the SKU has no stock record
     */
    Optional<Integer> available(String sku);

    /**
     * Takes units out of stock as part of the caller's transaction.
     *
     * @param sku the product code
     * @param quantity how many units to take
     * @throws OutOfStock if the SKU has fewer units than asked for
     * @throws StockConflict if another checkout changed the same row first
     */
    void reserve(String sku, int quantity);
}
```

Reading it, one piece at a time:

- **`StockRepository`**, a interface. Port: the units on hand per SKU.
  - **method `setAvailable`**: Sets the units on hand, creating the record if the SKU is new.
    - Signature: `void setAvailable(String sku, int available)`
    - input `sku`: the product code
    - input `available`: the units on hand
  - **method `available`**: Reports the units on hand.
    - Signature: `Optional<Integer> available(String sku)`
    - input `sku`: the product code
    - gives back: the units, or empty if the SKU has no stock record
  - **method `reserve`**: Takes units out of stock as part of the caller's transaction.
    - Signature: `void reserve(String sku, int quantity)`
    - input `sku`: the product code
    - input `quantity`: how many units to take
    - throws `OutOfStock`: if the SKU has fewer units than asked for
    - throws `StockConflict`: if another checkout changed the same row first

Language features in this file, explained:

- **interface**: An `interface` lists method signatures without bodies. Any class that says it `implements` the interface must provide those methods, so callers can depend on the list alone.
- **public**: `public` lets any class use the member.
- **Optional**: `Optional<T>` is a box that holds either one `T` or nothing. It makes "may be missing" visible in the type instead of using null.


### `src/main/java/dev/plainround/orders/infrastructure/ClockConfig.java`

Package `dev.plainround.orders.infrastructure`. The one place the real clock is created; tests replace this bean with a fixed one.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.infrastructure;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The one place the real clock is created; tests replace this bean with a fixed one. */
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

Reading it, one piece at a time:

- **`ClockConfig`**, a class. The one place the real clock is created; tests replace this bean with a fixed one.
  - annotation `@Configuration(proxyBeanMethods = false)`: marks a class whose `@Bean` methods define objects for Spring to manage
  - **method `clock`**: Provides the system clock in UTC.
    - Signature: `public Clock clock()`
    - gives back: the clock
    - annotation `@Bean`: tells Spring to call this method once at startup and keep the returned object as a shared, managed object
    - What the body does, in order:
      - returns `Clock.systemUTC()`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@Configuration`: marks a class whose `@Bean` methods define objects for Spring to manage.
- `@Bean`: tells Spring to call this method once at startup and keep the returned object as a shared, managed object.

Language features in this file, explained:

- **public**: `public` lets any class use the member.


### `src/main/java/dev/plainround/orders/infrastructure/persistence/AuditJpaRepository.java`

Package `dev.plainround.orders.infrastructure.persistence`. Spring Data access to the audit trail.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.infrastructure.persistence;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Spring Data access to the audit trail. */
public interface AuditJpaRepository extends JpaRepository<AuditRecordEntity, Long> {

    /**
     * Returns the newest entries first.
     *
     * @param page the size limit
     * @return the entries
     */
    @Query("select a from AuditRecordEntity a order by a.id desc")
    List<AuditRecordEntity> findLatest(Pageable page);
}
```

Reading it, one piece at a time:

- **`AuditJpaRepository`**, a interface that extends `JpaRepository<AuditRecordEntity, Long>`. Spring Data access to the audit trail.
  - **method `findLatest`**: Returns the newest entries first.
    - Signature: `List<AuditRecordEntity> findLatest(Pageable page)`
    - input `page`: the size limit
    - gives back: the entries
    - annotation `@Query("select a from AuditRecordEntity a order by a.id desc")`: Spring Data JPA: supplies the query text for this repository method

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@Query`: Spring Data JPA: supplies the query text for this repository method.

Language features in this file, explained:

- **interface**: An `interface` lists method signatures without bodies. Any class that says it `implements` the interface must provide those methods, so callers can depend on the list alone.
- **public**: `public` lets any class use the member.
- **extends**: `extends X` makes the class a child of `X`: it inherits what `X` has and may replace parts.
- **List**: A `List` is an ordered collection of items; `List.of(...)` builds one that cannot be changed afterwards.


### `src/main/java/dev/plainround/orders/infrastructure/persistence/AuditRecordEntity.java`

Package `dev.plainround.orders.infrastructure.persistence`. The `audit_records` table.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** The {@code audit_records} table. */
@Entity
@Table(name = "audit_records")
public class AuditRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String action;

    private String detail;

    private Instant recordedAt;

    /** For JPA. */
    protected AuditRecordEntity() {}

    /**
     * Creates an entry that is not stored yet.
     *
     * @param action what was attempted
     * @param detail who and what
     * @param recordedAt when it was written
     */
    public AuditRecordEntity(String action, String detail, Instant recordedAt) {
        this.action = action;
        this.detail = detail;
        this.recordedAt = recordedAt;
    }

    /**
     * Returns the generated id.
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns what was attempted.
     *
     * @return the action
     */
    public String getAction() {
        return action;
    }

    /**
     * Returns who and what.
     *
     * @return the detail
     */
    public String getDetail() {
        return detail;
    }

    /**
     * Returns when the entry was written.
     *
     * @return the instant
     */
    public Instant getRecordedAt() {
        return recordedAt;
    }
}
```

Reading it, one piece at a time:

- **`AuditRecordEntity`**, a class. The `audit_records` table.
  - annotation `@Entity`: JPA: marks a class whose objects are stored as rows of a table
  - annotation `@Table(name = "audit_records")`: JPA: names the database table that stores this entity
  - **method `id`**: the signature is `Long id`.
    - Signature: `private Long id`
    - annotation `@Id`: JPA: marks the field that is the primary key (the value that identifies one row)
    - annotation `@GeneratedValue(strategy = GenerationType.IDENTITY)`: JPA: the database or Hibernate produces this key value when a row is inserted
  - **field `action`** of type `String`, set once when the object is built.
    - Declaration: `private String action`
  - **field `detail`** of type `String`, set once when the object is built.
    - Declaration: `private String detail`
  - **field `recordedAt`** of type `Instant`, set once when the object is built.
    - Declaration: `private Instant recordedAt`
  - **method `AuditRecordEntity`**: For JPA.
    - Signature: `protected AuditRecordEntity()`
  - **method `AuditRecordEntity`**: Creates an entry that is not stored yet.
    - Signature: `public AuditRecordEntity(String action, String detail, Instant recordedAt)`
    - input `action`: what was attempted
    - input `detail`: who and what
    - input `recordedAt`: when it was written
    - What the body does, in order:
      - stores `action` in the field `action`
      - stores `detail` in the field `detail`
      - stores `recordedAt` in the field `recordedAt`
  - **method `getId`**: Returns the generated id.
    - Signature: `public Long getId()`
    - gives back: the id
    - What the body does, in order:
      - returns `id`
  - **method `getAction`**: Returns what was attempted.
    - Signature: `public String getAction()`
    - gives back: the action
    - What the body does, in order:
      - returns `action`
  - **method `getDetail`**: Returns who and what.
    - Signature: `public String getDetail()`
    - gives back: the detail
    - What the body does, in order:
      - returns `detail`
  - **method `getRecordedAt`**: Returns when the entry was written.
    - Signature: `public Instant getRecordedAt()`
    - gives back: the instant
    - What the body does, in order:
      - returns `recordedAt`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@Entity`: JPA: marks a class whose objects are stored as rows of a table.
- `@Table`: JPA: names the database table that stores this entity.
- `@Id`: JPA: marks the field that is the primary key (the value that identifies one row).
- `@GeneratedValue`: JPA: the database or Hibernate produces this key value when a row is inserted.

Language features in this file, explained:

- **private**: `private` hides a member from every other class.
- **public**: `public` lets any class use the member.


### `src/main/java/dev/plainround/orders/infrastructure/persistence/CustomerEntity.java`

Package `dev.plainround.orders.infrastructure.persistence`. The `customers` table.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.infrastructure.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/**
 * The {@code customers} table. Orders load lazily: JPA's default for a collection, stated for the
 * reader.
 */
@Entity
@Table(name = "customers")
public class CustomerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    // Deleting a customer deletes the orders: the customer owns their lifecycle.
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    private List<OrderEntity> orders = new ArrayList<>();

    /** For JPA. */
    protected CustomerEntity() {}

    /**
     * Creates a customer that is not stored yet.
     *
     * @param name the display name
     */
    public CustomerEntity(String name) {
        this.name = name;
    }

    /**
     * Returns the generated id.
     *
     * @return the id, null before the first save
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns the display name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the orders, loaded on first access.
     *
     * @return the live collection
     */
    public List<OrderEntity> getOrders() {
        return orders;
    }

    /**
     * Attaches an order and sets its owning side.
     *
     * @param order the order to attach
     */
    public void addOrder(OrderEntity order) {
        order.setCustomer(this);
        orders.add(order);
    }
}
```

Reading it, one piece at a time:

- **`CustomerEntity`**, a class. The `customers` table. Orders load lazily: JPA's default for a collection, stated for the reader.
  - annotation `@Entity`: JPA: marks a class whose objects are stored as rows of a table
  - annotation `@Table(name = "customers")`: JPA: names the database table that stores this entity
  - **method `id`**: the signature is `Long id`.
    - Signature: `private Long id`
    - annotation `@Id`: JPA: marks the field that is the primary key (the value that identifies one row)
    - annotation `@GeneratedValue(strategy = GenerationType.IDENTITY)`: JPA: the database or Hibernate produces this key value when a row is inserted
  - **field `name`** of type `String`, set once when the object is built.
    - Declaration: `private String name`
  - **method `ArrayList<>()`**: the signature is `List<OrderEntity> orders = new ArrayList<>()`.
    - Signature: `private List<OrderEntity> orders = new ArrayList<>()`
    - annotation `@OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)`: JPA: one row of this entity owns many rows of another entity
  - **method `CustomerEntity`**: For JPA.
    - Signature: `protected CustomerEntity()`
  - **method `CustomerEntity`**: Creates a customer that is not stored yet.
    - Signature: `public CustomerEntity(String name)`
    - input `name`: the display name
    - What the body does, in order:
      - stores `name` in the field `name`
  - **method `getId`**: Returns the generated id.
    - Signature: `public Long getId()`
    - gives back: the id, null before the first save
    - What the body does, in order:
      - returns `id`
  - **method `getName`**: Returns the display name.
    - Signature: `public String getName()`
    - gives back: the name
    - What the body does, in order:
      - returns `name`
  - **method `getOrders`**: Returns the orders, loaded on first access.
    - Signature: `public List<OrderEntity> getOrders()`
    - gives back: the live collection
    - What the body does, in order:
      - returns `orders`
  - **method `addOrder`**: Attaches an order and sets its owning side.
    - Signature: `public void addOrder(OrderEntity order)`
    - input `order`: the order to attach
    - What the body does, in order:
      - calls `order.setCustomer(this)`
      - calls `orders.add(order)`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@Entity`: JPA: marks a class whose objects are stored as rows of a table.
- `@Table`: JPA: names the database table that stores this entity.
- `@Id`: JPA: marks the field that is the primary key (the value that identifies one row).
- `@GeneratedValue`: JPA: the database or Hibernate produces this key value when a row is inserted.
- `@OneToMany`: JPA: one row of this entity owns many rows of another entity.

Language features in this file, explained:

- **private**: `private` hides a member from every other class.
- **public**: `public` lets any class use the member.
- **List**: A `List` is an ordered collection of items; `List.of(...)` builds one that cannot be changed afterwards.


### `src/main/java/dev/plainround/orders/infrastructure/persistence/CustomerJpaRepository.java`

Package `dev.plainround.orders.infrastructure.persistence`. Spring Data access to customers.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data access to customers. */
public interface CustomerJpaRepository extends JpaRepository<CustomerEntity, Long> {}
```

Reading it, one piece at a time:

- **`CustomerJpaRepository`**, a interface that extends `JpaRepository<CustomerEntity, Long>`. Spring Data access to customers.

Language features in this file, explained:

- **interface**: An `interface` lists method signatures without bodies. Any class that says it `implements` the interface must provide those methods, so callers can depend on the list alone.
- **public**: `public` lets any class use the member.
- **extends**: `extends X` makes the class a child of `X`: it inherits what `X` has and may replace parts.


### `src/main/java/dev/plainround/orders/infrastructure/persistence/JpaAuditLog.java`

Package `dev.plainround.orders.infrastructure.persistence`. Adapter: the audit trail.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.infrastructure.persistence;

import dev.plainround.orders.domain.AuditEntry;
import dev.plainround.orders.domain.AuditLog;
import java.time.Clock;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adapter: the audit trail. This is deliberately its own bean, not a method of the checkout
 * service. {@code REQUIRES_NEW} is applied by the Spring proxy around a bean, and a call from a
 * method of the same class never goes through that proxy, so there the annotation would be ignored
 * and the entry would roll back with the checkout it records. Callers reach this bean through its
 * proxy, so the entry commits on its own.
 */
@Component
public class JpaAuditLog implements AuditLog {

    private final AuditJpaRepository audits;
    private final Clock clock;

    /**
     * Wires the adapter.
     *
     * @param audits the Spring Data repository
     * @param clock the time source
     */
    public JpaAuditLog(AuditJpaRepository audits, Clock clock) {
        this.audits = audits;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String detail) {
        audits.save(new AuditRecordEntity(action, detail, clock.instant()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEntry> latest(int limit) {
        return audits.findLatest(PageRequest.of(0, limit)).stream()
                .map(
                        entity ->
                                new AuditEntry(
                                        entity.getId(),
                                        entity.getAction(),
                                        entity.getDetail(),
                                        entity.getRecordedAt()))
                .toList();
    }
}
```

Reading it, one piece at a time:

- **`JpaAuditLog`**, a class that implements `AuditLog`. Adapter: the audit trail. This is deliberately its own bean, not a method of the checkout service. `REQUIRES_NEW` is applied by the Spring proxy around a bean, and a call from a method of the same class never goes through that proxy, so there the annotation would be ignored and the entry would roll back with the checkout it records. Callers reach this bean through its proxy, so the entry commits on its own.
  - annotation `@Component`: tells Spring to create one shared instance of this class at startup and manage it
  - **field `audits`** of type `AuditJpaRepository`, set once when the object is built.
    - Declaration: `private final AuditJpaRepository audits`
  - **field `clock`** of type `Clock`, set once when the object is built.
    - Declaration: `private final Clock clock`
  - **method `JpaAuditLog`**: Wires the adapter.
    - Signature: `public JpaAuditLog(AuditJpaRepository audits, Clock clock)`
    - input `audits`: the Spring Data repository
    - input `clock`: the time source
    - What the body does, in order:
      - stores `audits` in the field `audits`
      - stores `clock` in the field `clock`
  - **method `record`**: the signature is `void record(String action, String detail)`.
    - Signature: `public void record(String action, String detail)`
    - annotation `@Override`: says this method replaces one with the same name from a parent class or interface; the compiler checks that such a parent method exists
    - annotation `@Transactional(propagation = Propagation.REQUIRES_NEW)`: wraps the call in a database transaction (a group of changes that all succeed or all roll back)
    - What the body does, in order:
      - calls `audits.save(new AuditRecordEntity(action, detail, clock.instant()))`
  - **method `latest`**: the signature is `List<AuditEntry> latest(int limit)`.
    - Signature: `public List<AuditEntry> latest(int limit)`
    - annotation `@Override`: says this method replaces one with the same name from a parent class or interface; the compiler checks that such a parent method exists
    - annotation `@Transactional(readOnly = true)`: wraps the call in a database transaction (a group of changes that all succeed or all roll back)
    - What the body does, in order:
      - returns `audits.findLatest(PageRequest.of(0, limit)).stream() .map( entity -> new AuditEntry( entity.getId(), entity.getAction(), entity.getDetail(), entity.getRecordedAt())) .toList()`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@Component`: tells Spring to create one shared instance of this class at startup and manage it.
- `@Override`: says this method replaces one with the same name from a parent class or interface; the compiler checks that such a parent method exists.
- `@Transactional`: wraps the call in a database transaction (a group of changes that all succeed or all roll back).

Language features in this file, explained:

- **final**: `final` on a field or variable means it is assigned once and never changed; on a class it forbids subclasses.
- **private**: `private` hides a member from every other class.
- **public**: `public` lets any class use the member.
- **implements**: `implements X` promises that the class provides every method of the interface `X`.
- **lambda (`->`)**: An arrow `a -> b` is a lambda: a small unnamed function. The part before the arrow lists its inputs and the part after is what it does.
- **List**: A `List` is an ordered collection of items; `List.of(...)` builds one that cannot be changed afterwards.
- **Stream**: A stream processes a collection step by step: `map` converts each item, `filter` keeps some, `toList` collects the result.


### `src/main/java/dev/plainround/orders/infrastructure/persistence/JpaCustomerRepository.java`

Package `dev.plainround.orders.infrastructure.persistence`. Adapter: the customer port on top of Spring Data JPA.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.infrastructure.persistence;

import dev.plainround.orders.domain.Customer;
import dev.plainround.orders.domain.CustomerRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Adapter: the customer port on top of Spring Data JPA. */
@Repository
public class JpaCustomerRepository implements CustomerRepository {

    private final CustomerJpaRepository customers;

    /**
     * Wires the adapter.
     *
     * @param customers the Spring Data repository
     */
    public JpaCustomerRepository(CustomerJpaRepository customers) {
        this.customers = customers;
    }

    @Override
    public Customer register(String name) {
        CustomerEntity saved = customers.save(new CustomerEntity(name));
        return new Customer(saved.getId(), saved.getName());
    }

    @Override
    public Optional<Customer> findById(long id) {
        return customers.findById(id).map(entity -> new Customer(entity.getId(), entity.getName()));
    }
}
```

Reading it, one piece at a time:

- **`JpaCustomerRepository`**, a class that implements `CustomerRepository`. Adapter: the customer port on top of Spring Data JPA.
  - annotation `@Repository`: marks this class or interface as a place that stores and fetches data; Spring manages it and translates storage errors into its own exceptions
  - **field `customers`** of type `CustomerJpaRepository`, set once when the object is built.
    - Declaration: `private final CustomerJpaRepository customers`
  - **method `JpaCustomerRepository`**: Wires the adapter.
    - Signature: `public JpaCustomerRepository(CustomerJpaRepository customers)`
    - input `customers`: the Spring Data repository
    - What the body does, in order:
      - stores `customers` in the field `customers`
  - **method `register`**: the signature is `Customer register(String name)`.
    - Signature: `public Customer register(String name)`
    - annotation `@Override`: says this method replaces one with the same name from a parent class or interface; the compiler checks that such a parent method exists
    - What the body does, in order:
      - creates the variable `saved` (of type `CustomerEntity`) holding `customers.save(new CustomerEntity(name))`
      - returns `new Customer(saved.getId(), saved.getName())`
  - **method `findById`**: the signature is `Optional<Customer> findById(long id)`.
    - Signature: `public Optional<Customer> findById(long id)`
    - annotation `@Override`: says this method replaces one with the same name from a parent class or interface; the compiler checks that such a parent method exists
    - What the body does, in order:
      - returns `customers.findById(id).map(entity -> new Customer(entity.getId(), entity.getName()))`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@Repository`: marks this class or interface as a place that stores and fetches data; Spring manages it and translates storage errors into its own exceptions.
- `@Override`: says this method replaces one with the same name from a parent class or interface; the compiler checks that such a parent method exists.

Language features in this file, explained:

- **final**: `final` on a field or variable means it is assigned once and never changed; on a class it forbids subclasses.
- **private**: `private` hides a member from every other class.
- **public**: `public` lets any class use the member.
- **implements**: `implements X` promises that the class provides every method of the interface `X`.
- **lambda (`->`)**: An arrow `a -> b` is a lambda: a small unnamed function. The part before the arrow lists its inputs and the part after is what it does.
- **Optional**: `Optional<T>` is a box that holds either one `T` or nothing. It makes "may be missing" visible in the type instead of using null.


### `src/main/java/dev/plainround/orders/infrastructure/persistence/JpaOrderRepository.java`

Package `dev.plainround.orders.infrastructure.persistence`. Adapter: the order port on top of Spring Data JPA.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.infrastructure.persistence;

import dev.plainround.orders.domain.Order;
import dev.plainround.orders.domain.OrderDraft;
import dev.plainround.orders.domain.OrderLine;
import dev.plainround.orders.domain.OrderRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Repository;

/** Adapter: the order port on top of Spring Data JPA. */
@Repository
public class JpaOrderRepository implements OrderRepository {

    private final OrderJpaRepository orders;
    private final CustomerJpaRepository customers;

    /**
     * Wires the adapter.
     *
     * @param orders the Spring Data repository for orders
     * @param customers the Spring Data repository for customers, used for a reference only
     */
    public JpaOrderRepository(OrderJpaRepository orders, CustomerJpaRepository customers) {
        this.orders = orders;
        this.customers = customers;
    }

    @Override
    public Order place(OrderDraft draft, Instant placedAt) {
        OrderEntity entity = new OrderEntity(placedAt);
        draft.lines()
                .forEach(line -> entity.addLine(new OrderLineEntity(line.sku(), line.quantity())));
        entity.setCustomer(customers.getReferenceById(draft.customerId()));
        return toDomain(orders.save(entity));
    }

    @Override
    public List<Order> findByCustomerId(long customerId) {
        return orders.findByCustomerIdWithLines(customerId).stream()
                .map(JpaOrderRepository::toDomain)
                .toList();
    }

    private static Order toDomain(OrderEntity entity) {
        List<OrderLine> lines =
                entity.getLines().stream()
                        .map(line -> new OrderLine(line.getSku(), line.getQuantity()))
                        .toList();
        return new Order(entity.getId(), entity.getCustomer().getId(), entity.getPlacedAt(), lines);
    }
}
```

Reading it, one piece at a time:

- **`JpaOrderRepository`**, a class that implements `OrderRepository`. Adapter: the order port on top of Spring Data JPA.
  - annotation `@Repository`: marks this class or interface as a place that stores and fetches data; Spring manages it and translates storage errors into its own exceptions
  - **field `orders`** of type `OrderJpaRepository`, set once when the object is built.
    - Declaration: `private final OrderJpaRepository orders`
  - **field `customers`** of type `CustomerJpaRepository`, set once when the object is built.
    - Declaration: `private final CustomerJpaRepository customers`
  - **method `JpaOrderRepository`**: Wires the adapter.
    - Signature: `public JpaOrderRepository(OrderJpaRepository orders, CustomerJpaRepository customers)`
    - input `orders`: the Spring Data repository for orders
    - input `customers`: the Spring Data repository for customers, used for a reference only
    - What the body does, in order:
      - stores `orders` in the field `orders`
      - stores `customers` in the field `customers`
  - **method `place`**: the signature is `Order place(OrderDraft draft, Instant placedAt)`.
    - Signature: `public Order place(OrderDraft draft, Instant placedAt)`
    - annotation `@Override`: says this method replaces one with the same name from a parent class or interface; the compiler checks that such a parent method exists
    - What the body does, in order:
      - creates the variable `entity` (of type `OrderEntity`) holding `new OrderEntity(placedAt)`
      - calls `draft.lines() .forEach(line -> entity.addLine(new OrderLineEntity(line.sku(), line.quantity())))`
      - calls `entity.setCustomer(customers.getReferenceById(draft.customerId()))`
      - returns `toDomain(orders.save(entity))`
  - **method `findByCustomerId`**: the signature is `List<Order> findByCustomerId(long customerId)`.
    - Signature: `public List<Order> findByCustomerId(long customerId)`
    - annotation `@Override`: says this method replaces one with the same name from a parent class or interface; the compiler checks that such a parent method exists
    - What the body does, in order:
      - returns `orders.findByCustomerIdWithLines(customerId).stream() .map(JpaOrderRepository::toDomain) .toList()`
  - **method `toDomain`**: the signature is `static Order toDomain(OrderEntity entity)`.
    - Signature: `private static Order toDomain(OrderEntity entity)`
    - What the body does, in order:
      - creates the variable `lines` (of type `List<OrderLine>`) holding `entity.getLines().stream() .map(line -> new OrderLine(line.getSku(), line.getQuantity())) .toList()`
      - returns `new Order(entity.getId(), entity.getCustomer().getId(), entity.getPlacedAt(), lines)`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@Repository`: marks this class or interface as a place that stores and fetches data; Spring manages it and translates storage errors into its own exceptions.
- `@Override`: says this method replaces one with the same name from a parent class or interface; the compiler checks that such a parent method exists.

Language features in this file, explained:

- **final**: `final` on a field or variable means it is assigned once and never changed; on a class it forbids subclasses.
- **static**: `static` means the member belongs to the class itself, not to one object; you use it without creating an object.
- **private**: `private` hides a member from every other class.
- **public**: `public` lets any class use the member.
- **implements**: `implements X` promises that the class provides every method of the interface `X`.
- **lambda (`->`)**: An arrow `a -> b` is a lambda: a small unnamed function. The part before the arrow lists its inputs and the part after is what it does.
- **method reference (`::`)**: `Type::method` is a short way to pass an existing method where a function is expected.
- **List**: A `List` is an ordered collection of items; `List.of(...)` builds one that cannot be changed afterwards.
- **Stream**: A stream processes a collection step by step: `map` converts each item, `filter` keeps some, `toList` collects the result.


### `src/main/java/dev/plainround/orders/infrastructure/persistence/JpaStockRepository.java`

Package `dev.plainround.orders.infrastructure.persistence`. Adapter: the stock port on top of Spring Data JPA, with optimistic locking on every change.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.infrastructure.persistence;

import dev.plainround.orders.domain.OutOfStock;
import dev.plainround.orders.domain.StockConflict;
import dev.plainround.orders.domain.StockRepository;
import java.util.Optional;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

/** Adapter: the stock port on top of Spring Data JPA, with optimistic locking on every change. */
@Repository
public class JpaStockRepository implements StockRepository {

    private final StockJpaRepository stock;

    /**
     * Wires the adapter.
     *
     * @param stock the Spring Data repository
     */
    public JpaStockRepository(StockJpaRepository stock) {
        this.stock = stock;
    }

    @Override
    public void setAvailable(String sku, int available) {
        StockItemEntity item =
                stock.findById(sku).orElseGet(() -> new StockItemEntity(sku, available));
        item.setAvailable(available);
        stock.save(item);
    }

    @Override
    public Optional<Integer> available(String sku) {
        return stock.findById(sku).map(StockItemEntity::getAvailable);
    }

    @Override
    public void reserve(String sku, int quantity) {
        StockItemEntity item = stock.findById(sku).orElseThrow(() -> new OutOfStock(sku));
        if (item.getAvailable() < quantity) {
            throw new OutOfStock(sku);
        }
        item.setAvailable(item.getAvailable() - quantity);
        flushOrConflict(item, sku);
    }

    private void flushOrConflict(StockItemEntity item, String sku) {
        try {
            stock.saveAndFlush(item);
        } catch (ObjectOptimisticLockingFailureException conflict) {
            throw new StockConflict(sku);
        }
    }
}
```

Reading it, one piece at a time:

- **`JpaStockRepository`**, a class that implements `StockRepository`. Adapter: the stock port on top of Spring Data JPA, with optimistic locking on every change.
  - annotation `@Repository`: marks this class or interface as a place that stores and fetches data; Spring manages it and translates storage errors into its own exceptions
  - **field `stock`** of type `StockJpaRepository`, set once when the object is built.
    - Declaration: `private final StockJpaRepository stock`
  - **method `JpaStockRepository`**: Wires the adapter.
    - Signature: `public JpaStockRepository(StockJpaRepository stock)`
    - input `stock`: the Spring Data repository
    - What the body does, in order:
      - stores `stock` in the field `stock`
  - **method `setAvailable`**: the signature is `void setAvailable(String sku, int available)`.
    - Signature: `public void setAvailable(String sku, int available)`
    - annotation `@Override`: says this method replaces one with the same name from a parent class or interface; the compiler checks that such a parent method exists
    - What the body does, in order:
      - creates the variable `item` (of type `StockItemEntity`) holding `stock.findById(sku).orElseGet(() -> new StockItemEntity(sku, available))`
      - calls `item.setAvailable(available)`
      - calls `stock.save(item)`
  - **method `available`**: the signature is `Optional<Integer> available(String sku)`.
    - Signature: `public Optional<Integer> available(String sku)`
    - annotation `@Override`: says this method replaces one with the same name from a parent class or interface; the compiler checks that such a parent method exists
    - What the body does, in order:
      - returns `stock.findById(sku).map(StockItemEntity::getAvailable)`
  - **method `reserve`**: the signature is `void reserve(String sku, int quantity)`.
    - Signature: `public void reserve(String sku, int quantity)`
    - annotation `@Override`: says this method replaces one with the same name from a parent class or interface; the compiler checks that such a parent method exists
    - What the body does, in order:
      - creates the variable `item` (of type `StockItemEntity`) holding `stock.findById(sku).orElseThrow(() -> new OutOfStock(sku))`
      - when `item.getAvailable() < quantity` is true:
        - throws a new `OutOfStock` created with `sku`
      - calls `item.setAvailable(item.getAvailable() - quantity)`
      - calls `flushOrConflict(item, sku)`
  - **method `flushOrConflict`**: the signature is `void flushOrConflict(StockItemEntity item, String sku)`.
    - Signature: `private void flushOrConflict(StockItemEntity item, String sku)`
    - What the body does, in order:
      - tries this:
        - calls `stock.saveAndFlush(item)`
      - if this fails with `ObjectOptimisticLockingFailureException conflict`:
        - throws a new `StockConflict` created with `sku`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@Repository`: marks this class or interface as a place that stores and fetches data; Spring manages it and translates storage errors into its own exceptions.
- `@Override`: says this method replaces one with the same name from a parent class or interface; the compiler checks that such a parent method exists.

Language features in this file, explained:

- **final**: `final` on a field or variable means it is assigned once and never changed; on a class it forbids subclasses.
- **private**: `private` hides a member from every other class.
- **public**: `public` lets any class use the member.
- **implements**: `implements X` promises that the class provides every method of the interface `X`.
- **lambda (`->`)**: An arrow `a -> b` is a lambda: a small unnamed function. The part before the arrow lists its inputs and the part after is what it does.
- **method reference (`::`)**: `Type::method` is a short way to pass an existing method where a function is expected.
- **Optional**: `Optional<T>` is a box that holds either one `T` or nothing. It makes "may be missing" visible in the type instead of using null.
- **try/catch**: `try { ... } catch (X e) { ... }` runs the first block and, if it throws `X`, runs the second.


### `src/main/java/dev/plainround/orders/infrastructure/persistence/OrderEntity.java`

Package `dev.plainround.orders.infrastructure.persistence`. The `orders_table` table ("order" is a reserved SQL word).

The whole file, as it is in the repository:

```java
package dev.plainround.orders.infrastructure.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** The {@code orders_table} table ("order" is a reserved SQL word). Both sides are lazy. */
@Entity
@Table(name = "orders_table")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @ManyToOne is eager by default; lazy is stated, so a load of an order never drags the
    // customer in.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private CustomerEntity customer;

    private Instant placedAt;

    // Cascade follows ownership: the order owns its lines, the lines do not cascade back up.
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    @OrderBy("id")
    private List<OrderLineEntity> lines = new ArrayList<>();

    /** For JPA. */
    protected OrderEntity() {}

    /**
     * Creates an order that is not stored yet.
     *
     * @param placedAt when the order was placed
     */
    public OrderEntity(Instant placedAt) {
        this.placedAt = placedAt;
    }

    /**
     * Returns the generated id.
     *
     * @return the id, null before the first save
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns the buyer, loaded on first access.
     *
     * @return the customer
     */
    public CustomerEntity getCustomer() {
        return customer;
    }

    /**
     * Sets the buyer, the side that owns the foreign key.
     *
     * @param customer the buyer
     */
    public void setCustomer(CustomerEntity customer) {
        this.customer = customer;
    }

    /**
     * Returns when the order was placed.
     *
     * @return the instant
     */
    public Instant getPlacedAt() {
        return placedAt;
    }

    /**
     * Returns the lines, loaded on first access unless a fetch join or entity graph loaded them.
     *
     * @return the live collection
     */
    public List<OrderLineEntity> getLines() {
        return lines;
    }

    /**
     * Attaches a line and sets its owning side.
     *
     * @param line the line to attach
     */
    public void addLine(OrderLineEntity line) {
        line.setOrder(this);
        lines.add(line);
    }
}
```

Reading it, one piece at a time:

- **`OrderEntity`**, a class. The `orders_table` table ("order" is a reserved SQL word). Both sides are lazy.
  - annotation `@Entity`: JPA: marks a class whose objects are stored as rows of a table
  - annotation `@Table(name = "orders_table")`: JPA: names the database table that stores this entity
  - **method `id`**: the signature is `Long id`.
    - Signature: `private Long id`
    - annotation `@Id`: JPA: marks the field that is the primary key (the value that identifies one row)
    - annotation `@GeneratedValue(strategy = GenerationType.IDENTITY)`: JPA: the database or Hibernate produces this key value when a row is inserted
  - **method `customer`**: the signature is `CustomerEntity customer`.
    - Signature: `private CustomerEntity customer`
    - annotation `@ManyToOne(fetch = FetchType.LAZY)`: JPA: many rows of this entity point to one row of another entity (a foreign key column)
    - annotation `@JoinColumn(name = "customer_id")`: JPA: names the foreign key column that links the two tables
  - **field `placedAt`** of type `Instant`, set once when the object is built.
    - Declaration: `private Instant placedAt`
  - **method `ArrayList<>()`**: the signature is `List<OrderLineEntity> lines = new ArrayList<>()`.
    - Signature: `private List<OrderLineEntity> lines = new ArrayList<>()`
    - annotation `@OneToMany(mappedBy = "order", cascade = CascadeType.ALL)`: JPA: one row of this entity owns many rows of another entity
    - annotation `@OrderBy("id")`: JPA: the order in which the collection is loaded
  - **method `OrderEntity`**: For JPA.
    - Signature: `protected OrderEntity()`
  - **method `OrderEntity`**: Creates an order that is not stored yet.
    - Signature: `public OrderEntity(Instant placedAt)`
    - input `placedAt`: when the order was placed
    - What the body does, in order:
      - stores `placedAt` in the field `placedAt`
  - **method `getId`**: Returns the generated id.
    - Signature: `public Long getId()`
    - gives back: the id, null before the first save
    - What the body does, in order:
      - returns `id`
  - **method `getCustomer`**: Returns the buyer, loaded on first access.
    - Signature: `public CustomerEntity getCustomer()`
    - gives back: the customer
    - What the body does, in order:
      - returns `customer`
  - **method `setCustomer`**: Sets the buyer, the side that owns the foreign key.
    - Signature: `public void setCustomer(CustomerEntity customer)`
    - input `customer`: the buyer
    - What the body does, in order:
      - stores `customer` in the field `customer`
  - **method `getPlacedAt`**: Returns when the order was placed.
    - Signature: `public Instant getPlacedAt()`
    - gives back: the instant
    - What the body does, in order:
      - returns `placedAt`
  - **method `getLines`**: Returns the lines, loaded on first access unless a fetch join or entity graph loaded them.
    - Signature: `public List<OrderLineEntity> getLines()`
    - gives back: the live collection
    - What the body does, in order:
      - returns `lines`
  - **method `addLine`**: Attaches a line and sets its owning side.
    - Signature: `public void addLine(OrderLineEntity line)`
    - input `line`: the line to attach
    - What the body does, in order:
      - calls `line.setOrder(this)`
      - calls `lines.add(line)`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@Entity`: JPA: marks a class whose objects are stored as rows of a table.
- `@Table`: JPA: names the database table that stores this entity.
- `@Id`: JPA: marks the field that is the primary key (the value that identifies one row).
- `@GeneratedValue`: JPA: the database or Hibernate produces this key value when a row is inserted.
- `@ManyToOne`: JPA: many rows of this entity point to one row of another entity (a foreign key column).
- `@JoinColumn`: JPA: names the foreign key column that links the two tables.
- `@OneToMany`: JPA: one row of this entity owns many rows of another entity.
- `@OrderBy`: JPA: the order in which the collection is loaded.

Language features in this file, explained:

- **private**: `private` hides a member from every other class.
- **public**: `public` lets any class use the member.
- **List**: A `List` is an ordered collection of items; `List.of(...)` builds one that cannot be changed afterwards.


### `src/main/java/dev/plainround/orders/infrastructure/persistence/OrderJpaRepository.java`

Package `dev.plainround.orders.infrastructure.persistence`. Spring Data access to orders.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data access to orders. */
public interface OrderJpaRepository extends JpaRepository<OrderEntity, Long> {

    /**
     * Loads a customer's orders and their lines in one SQL statement. The entity graph turns the
     * lazy {@code lines} collection into a join, so the statement count does not grow with the
     * number of orders.
     *
     * @param customerId the buyer
     * @return the orders, oldest first, with lines loaded
     */
    @EntityGraph(attributePaths = "lines")
    @Query("select o from OrderEntity o where o.customer.id = :customerId order by o.id")
    List<OrderEntity> findByCustomerIdWithLines(@Param("customerId") Long customerId);
}
```

Reading it, one piece at a time:

- **`OrderJpaRepository`**, a interface that extends `JpaRepository<OrderEntity, Long>`. Spring Data access to orders.
  - **method `findByCustomerIdWithLines`**: Loads a customer's orders and their lines in one SQL statement. The entity graph turns the lazy `lines` collection into a join, so the statement count does not grow with the number of orders.
    - Signature: `List<OrderEntity> findByCustomerIdWithLines(@Param("customerId") Long customerId)`
    - input `customerId`: the buyer
    - gives back: the orders, oldest first, with lines loaded
    - annotation `@EntityGraph(attributePaths = "lines")`: Spring Data JPA: loads the named related entities in the same query as this one
    - annotation `@Query("select o from OrderEntity o where o.customer.id = :customerId order by o.id")`: Spring Data JPA: supplies the query text for this repository method

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@EntityGraph`: Spring Data JPA: loads the named related entities in the same query as this one.
- `@Query`: Spring Data JPA: supplies the query text for this repository method.
- `@Param`: Spring Data: names a query parameter so the `:name` in the query text can find it.

Language features in this file, explained:

- **interface**: An `interface` lists method signatures without bodies. Any class that says it `implements` the interface must provide those methods, so callers can depend on the list alone.
- **public**: `public` lets any class use the member.
- **extends**: `extends X` makes the class a child of `X`: it inherits what `X` has and may replace parts.
- **List**: A `List` is an ordered collection of items; `List.of(...)` builds one that cannot be changed afterwards.


### `src/main/java/dev/plainround/orders/infrastructure/persistence/OrderLineEntity.java`

Package `dev.plainround.orders.infrastructure.persistence`. The `order_lines` table.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** The {@code order_lines} table. */
@Entity
@Table(name = "order_lines")
public class OrderLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private OrderEntity order;

    private String sku;

    private int quantity;

    /** For JPA. */
    protected OrderLineEntity() {}

    /**
     * Creates a line that is not attached yet.
     *
     * @param sku the product code
     * @param quantity the number of units
     */
    public OrderLineEntity(String sku, int quantity) {
        this.sku = sku;
        this.quantity = quantity;
    }

    /**
     * Returns the generated id.
     *
     * @return the id, null before the first save
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns the order this line belongs to.
     *
     * @return the order
     */
    public OrderEntity getOrder() {
        return order;
    }

    /**
     * Sets the order, the side that owns the foreign key.
     *
     * @param order the order
     */
    public void setOrder(OrderEntity order) {
        this.order = order;
    }

    /**
     * Returns the product code.
     *
     * @return the SKU
     */
    public String getSku() {
        return sku;
    }

    /**
     * Returns the number of units.
     *
     * @return the quantity
     */
    public int getQuantity() {
        return quantity;
    }
}
```

Reading it, one piece at a time:

- **`OrderLineEntity`**, a class. The `order_lines` table.
  - annotation `@Entity`: JPA: marks a class whose objects are stored as rows of a table
  - annotation `@Table(name = "order_lines")`: JPA: names the database table that stores this entity
  - **method `id`**: the signature is `Long id`.
    - Signature: `private Long id`
    - annotation `@Id`: JPA: marks the field that is the primary key (the value that identifies one row)
    - annotation `@GeneratedValue(strategy = GenerationType.IDENTITY)`: JPA: the database or Hibernate produces this key value when a row is inserted
  - **method `order`**: the signature is `OrderEntity order`.
    - Signature: `private OrderEntity order`
    - annotation `@ManyToOne(fetch = FetchType.LAZY)`: JPA: many rows of this entity point to one row of another entity (a foreign key column)
    - annotation `@JoinColumn(name = "order_id")`: JPA: names the foreign key column that links the two tables
  - **field `sku`** of type `String`, set once when the object is built.
    - Declaration: `private String sku`
  - **field `quantity`** of type `int`, set once when the object is built.
    - Declaration: `private int quantity`
  - **method `OrderLineEntity`**: For JPA.
    - Signature: `protected OrderLineEntity()`
  - **method `OrderLineEntity`**: Creates a line that is not attached yet.
    - Signature: `public OrderLineEntity(String sku, int quantity)`
    - input `sku`: the product code
    - input `quantity`: the number of units
    - What the body does, in order:
      - stores `sku` in the field `sku`
      - stores `quantity` in the field `quantity`
  - **method `getId`**: Returns the generated id.
    - Signature: `public Long getId()`
    - gives back: the id, null before the first save
    - What the body does, in order:
      - returns `id`
  - **method `getOrder`**: Returns the order this line belongs to.
    - Signature: `public OrderEntity getOrder()`
    - gives back: the order
    - What the body does, in order:
      - returns `order`
  - **method `setOrder`**: Sets the order, the side that owns the foreign key.
    - Signature: `public void setOrder(OrderEntity order)`
    - input `order`: the order
    - What the body does, in order:
      - stores `order` in the field `order`
  - **method `getSku`**: Returns the product code.
    - Signature: `public String getSku()`
    - gives back: the SKU
    - What the body does, in order:
      - returns `sku`
  - **method `getQuantity`**: Returns the number of units.
    - Signature: `public int getQuantity()`
    - gives back: the quantity
    - What the body does, in order:
      - returns `quantity`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@Entity`: JPA: marks a class whose objects are stored as rows of a table.
- `@Table`: JPA: names the database table that stores this entity.
- `@Id`: JPA: marks the field that is the primary key (the value that identifies one row).
- `@GeneratedValue`: JPA: the database or Hibernate produces this key value when a row is inserted.
- `@ManyToOne`: JPA: many rows of this entity point to one row of another entity (a foreign key column).
- `@JoinColumn`: JPA: names the foreign key column that links the two tables.

Language features in this file, explained:

- **private**: `private` hides a member from every other class.
- **public**: `public` lets any class use the member.


### `src/main/java/dev/plainround/orders/infrastructure/persistence/StockItemEntity.java`

Package `dev.plainround.orders.infrastructure.persistence`. The `stock_items` table; `version` makes concurrent updates of one row fail loudly.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * The {@code stock_items} table; {@code version} makes concurrent updates of one row fail loudly.
 */
@Entity
@Table(name = "stock_items")
public class StockItemEntity {

    @Id private String sku;

    private int available;

    @Version private long version;

    /** For JPA. */
    protected StockItemEntity() {}

    /**
     * Creates a stock record.
     *
     * @param sku the product code
     * @param available the units on hand
     */
    public StockItemEntity(String sku, int available) {
        this.sku = sku;
        this.available = available;
    }

    /**
     * Returns the product code.
     *
     * @return the SKU
     */
    public String getSku() {
        return sku;
    }

    /**
     * Returns the units on hand.
     *
     * @return the units
     */
    public int getAvailable() {
        return available;
    }

    /**
     * Replaces the units on hand.
     *
     * @param available the new level
     */
    public void setAvailable(int available) {
        this.available = available;
    }

    /**
     * Returns the optimistic-lock version.
     *
     * @return the version
     */
    public long getVersion() {
        return version;
    }
}
```

Reading it, one piece at a time:

- **`StockItemEntity`**, a class. The `stock_items` table; `version` makes concurrent updates of one row fail loudly.
  - annotation `@Entity`: JPA: marks a class whose objects are stored as rows of a table
  - annotation `@Table(name = "stock_items")`: JPA: names the database table that stores this entity
  - **field `sku`** of type `String`, set once when the object is built.
    - Declaration: `private String sku`
    - annotation `@Id`: JPA: marks the field that is the primary key (the value that identifies one row)
  - **field `available`** of type `int`, set once when the object is built.
    - Declaration: `private int available`
  - **field `version`** of type `long`, set once when the object is built.
    - Declaration: `private long version`
    - annotation `@Version`: JPA: this field counts how many times the row changed; saving a stale copy fails (optimistic locking)
  - **method `StockItemEntity`**: For JPA.
    - Signature: `protected StockItemEntity()`
  - **method `StockItemEntity`**: Creates a stock record.
    - Signature: `public StockItemEntity(String sku, int available)`
    - input `sku`: the product code
    - input `available`: the units on hand
    - What the body does, in order:
      - stores `sku` in the field `sku`
      - stores `available` in the field `available`
  - **method `getSku`**: Returns the product code.
    - Signature: `public String getSku()`
    - gives back: the SKU
    - What the body does, in order:
      - returns `sku`
  - **method `getAvailable`**: Returns the units on hand.
    - Signature: `public int getAvailable()`
    - gives back: the units
    - What the body does, in order:
      - returns `available`
  - **method `setAvailable`**: Replaces the units on hand.
    - Signature: `public void setAvailable(int available)`
    - input `available`: the new level
    - What the body does, in order:
      - stores `available` in the field `available`
  - **method `getVersion`**: Returns the optimistic-lock version.
    - Signature: `public long getVersion()`
    - gives back: the version
    - What the body does, in order:
      - returns `version`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@Entity`: JPA: marks a class whose objects are stored as rows of a table.
- `@Table`: JPA: names the database table that stores this entity.
- `@Id`: JPA: marks the field that is the primary key (the value that identifies one row).
- `@Version`: JPA: this field counts how many times the row changed; saving a stale copy fails (optimistic locking).

Language features in this file, explained:

- **private**: `private` hides a member from every other class.
- **public**: `public` lets any class use the member.


### `src/main/java/dev/plainround/orders/infrastructure/persistence/StockJpaRepository.java`

Package `dev.plainround.orders.infrastructure.persistence`. Spring Data access to stock records.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data access to stock records. */
public interface StockJpaRepository extends JpaRepository<StockItemEntity, String> {}
```

Reading it, one piece at a time:

- **`StockJpaRepository`**, a interface that extends `JpaRepository<StockItemEntity, String>`. Spring Data access to stock records.

Language features in this file, explained:

- **interface**: An `interface` lists method signatures without bodies. Any class that says it `implements` the interface must provide those methods, so callers can depend on the list alone.
- **public**: `public` lets any class use the member.
- **extends**: `extends X` makes the class a child of `X`: it inherits what `X` has and may replace parts.


### `src/main/java/dev/plainround/orders/interfaces/web/AuditController.java`

Package `dev.plainround.orders.interfaces.web`. HTTP face of the audit trail (read only).

The whole file, as it is in the repository:

```java
package dev.plainround.orders.interfaces.web;

import dev.plainround.orders.application.AuditService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** HTTP face of the audit trail (read only). */
@RestController
@RequestMapping("/audit")
public class AuditController {

    private final AuditService service;

    /**
     * Wires the controller.
     *
     * @param service the audit queries
     */
    public AuditController(AuditService service) {
        this.service = service;
    }

    /**
     * Returns the newest audit entries first.
     *
     * @param limit how many at most, 1 to 100
     * @return the entries
     */
    @GetMapping
    public List<AuditResponse> latest(
            @RequestParam(defaultValue = "20")
                    @Min(value = 1, message = "limit must be at least 1")
                    @Max(value = 100, message = "limit must be at most 100")
                    int limit) {
        return service.latest(limit).stream().map(AuditResponse::from).toList();
    }
}
```

Reading it, one piece at a time:

- **`AuditController`**, a class. HTTP face of the audit trail (read only).
  - annotation `@RestController`: marks a class whose methods answer web requests, with return values written as JSON
  - annotation `@RequestMapping("/audit")`: sets the URL path prefix for every handler method in this class (or the path and verb of one method)
  - **field `service`** of type `AuditService`, set once when the object is built.
    - Declaration: `private final AuditService service`
  - **method `AuditController`**: Wires the controller.
    - Signature: `public AuditController(AuditService service)`
    - input `service`: the audit queries
    - What the body does, in order:
      - stores `service` in the field `service`
  - **method `latest`**: Returns the newest audit entries first.
    - Signature: `public List<AuditResponse> latest( @RequestParam(defaultValue = "20") @Min(value = 1, message = "limit must be at least 1") @Max(value = 100, message = "limit must be at most 100") int limit)`
    - input `limit`: how many at most, 1 to 100
    - gives back: the entries
    - annotation `@GetMapping`: routes HTTP GET requests for the given path to this method
    - What the body does, in order:
      - returns `service.latest(limit).stream().map(AuditResponse::from).toList()`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@RestController`: marks a class whose methods answer web requests, with return values written as JSON.
- `@RequestMapping`: sets the URL path prefix for every handler method in this class (or the path and verb of one method).
- `@GetMapping`: routes HTTP GET requests for the given path to this method.
- `@RequestParam`: fills this parameter from the query string (the part of the URL after the question mark).
- `@Min`: Bean Validation: the number must be at least the given value.
- `@Max`: Bean Validation: the number must be at most the given value.

Language features in this file, explained:

- **final**: `final` on a field or variable means it is assigned once and never changed; on a class it forbids subclasses.
- **private**: `private` hides a member from every other class.
- **public**: `public` lets any class use the member.
- **method reference (`::`)**: `Type::method` is a short way to pass an existing method where a function is expected.
- **List**: A `List` is an ordered collection of items; `List.of(...)` builds one that cannot be changed afterwards.
- **Stream**: A stream processes a collection step by step: `map` converts each item, `filter` keeps some, `toList` collects the result.


### `src/main/java/dev/plainround/orders/interfaces/web/AuditResponse.java`

Package `dev.plainround.orders.interfaces.web`. An audit entry as the API shows it.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.interfaces.web;

import dev.plainround.orders.domain.AuditEntry;
import java.time.Instant;

/**
 * An audit entry as the API shows it.
 *
 * @param id the identifier
 * @param action what was attempted
 * @param detail who and what
 * @param recordedAt when it was written
 */
public record AuditResponse(long id, String action, String detail, Instant recordedAt) {

    /**
     * Converts a domain entry.
     *
     * @param entry the entry
     * @return its API shape
     */
    public static AuditResponse from(AuditEntry entry) {
        return new AuditResponse(entry.id(), entry.action(), entry.detail(), entry.recordedAt());
    }
}
```

Reading it, one piece at a time:

- **`AuditResponse`**, a record carrying `long id, String action, String detail, Instant recordedAt`. An audit entry as the API shows it.
  - **method `from`**: Converts a domain entry.
    - Signature: `public static AuditResponse from(AuditEntry entry)`
    - input `entry`: the entry
    - gives back: its API shape
    - What the body does, in order:
      - returns `new AuditResponse(entry.id(), entry.action(), entry.detail(), entry.recordedAt())`

Language features in this file, explained:

- **record**: A `record` is a short way to declare a class that only carries data. The names in the parentheses become fields, a constructor, read methods with the same names, and `equals`, `hashCode` and `toString`.
- **static**: `static` means the member belongs to the class itself, not to one object; you use it without creating an object.
- **public**: `public` lets any class use the member.


### `src/main/java/dev/plainround/orders/interfaces/web/CheckoutRequest.java`

Package `dev.plainround.orders.interfaces.web`. The body of `POST /customers/{id`/orders}.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.interfaces.web;

import dev.plainround.orders.domain.OrderDraft;
import dev.plainround.orders.domain.OrderLine;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * The body of {@code POST /customers/{id}/orders}.
 *
 * @param lines what is being bought
 */
public record CheckoutRequest(
        @NotEmpty(message = "lines must not be empty")
                @Size(max = OrderDraft.MAX_LINES, message = "lines must have at most 50 entries")
                List<@Valid LineRequest> lines) {

    /**
     * Converts to domain lines.
     *
     * @return the lines
     */
    public List<OrderLine> toDomain() {
        return lines.stream().map(LineRequest::toDomain).toList();
    }
}
```

Reading it, one piece at a time:

- **`CheckoutRequest`**, a record carrying `@NotEmpty(message = "lines must not be empty") @Size(max = OrderDraft.MAX_LINES, message = "lines must have at most 50 entries") List<@Valid LineRequest> lines`. The body of `POST /customers/{id`/orders}.
  - **method `toDomain`**: Converts to domain lines.
    - Signature: `public List<OrderLine> toDomain()`
    - gives back: the lines
    - What the body does, in order:
      - returns `lines.stream().map(LineRequest::toDomain).toList()`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@NotEmpty`: Bean Validation: the text or collection must not be null or empty.
- `@Size`: Bean Validation: the text or collection length must lie between the given bounds.
- `@Valid`: tells Spring to run the Bean Validation rules on this object (and the objects inside it) before going on.

Language features in this file, explained:

- **record**: A `record` is a short way to declare a class that only carries data. The names in the parentheses become fields, a constructor, read methods with the same names, and `equals`, `hashCode` and `toString`.
- **public**: `public` lets any class use the member.
- **method reference (`::`)**: `Type::method` is a short way to pass an existing method where a function is expected.
- **List**: A `List` is an ordered collection of items; `List.of(...)` builds one that cannot be changed afterwards.
- **Stream**: A stream processes a collection step by step: `map` converts each item, `filter` keeps some, `toList` collects the result.


### `src/main/java/dev/plainround/orders/interfaces/web/CustomerController.java`

Package `dev.plainround.orders.interfaces.web`. HTTP face of the customers.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.interfaces.web;

import dev.plainround.orders.application.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** HTTP face of the customers. */
@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService service;

    /**
     * Wires the controller.
     *
     * @param service the customer use cases
     */
    public CustomerController(CustomerService service) {
        this.service = service;
    }

    /**
     * Registers a customer.
     *
     * @param request the validated body
     * @return the stored customer
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse register(@Valid @RequestBody NewCustomerRequest request) {
        return CustomerResponse.from(service.register(request.name()));
    }

    /**
     * Returns a customer.
     *
     * @param id the customer id
     * @return the customer
     */
    @GetMapping("/{id}")
    public CustomerResponse get(@PathVariable long id) {
        return CustomerResponse.from(service.require(id));
    }
}
```

Reading it, one piece at a time:

- **`CustomerController`**, a class. HTTP face of the customers.
  - annotation `@RestController`: marks a class whose methods answer web requests, with return values written as JSON
  - annotation `@RequestMapping("/customers")`: sets the URL path prefix for every handler method in this class (or the path and verb of one method)
  - **field `service`** of type `CustomerService`, set once when the object is built.
    - Declaration: `private final CustomerService service`
  - **method `CustomerController`**: Wires the controller.
    - Signature: `public CustomerController(CustomerService service)`
    - input `service`: the customer use cases
    - What the body does, in order:
      - stores `service` in the field `service`
  - **method `register`**: Registers a customer.
    - Signature: `public CustomerResponse register(@Valid @RequestBody NewCustomerRequest request)`
    - input `request`: the validated body
    - gives back: the stored customer
    - annotation `@PostMapping`: routes HTTP POST requests for the given path to this method
    - annotation `@ResponseStatus(HttpStatus.CREATED)`: sets the HTTP status code of the response, instead of the default 200
    - What the body does, in order:
      - returns `CustomerResponse.from(service.register(request.name()))`
  - **method `get`**: Returns a customer.
    - Signature: `public CustomerResponse get(@PathVariable long id)`
    - input `id`: the customer id
    - gives back: the customer
    - annotation `@GetMapping("/{id}")`: routes HTTP GET requests for the given path to this method
    - What the body does, in order:
      - returns `CustomerResponse.from(service.require(id))`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@RestController`: marks a class whose methods answer web requests, with return values written as JSON.
- `@RequestMapping`: sets the URL path prefix for every handler method in this class (or the path and verb of one method).
- `@PostMapping`: routes HTTP POST requests for the given path to this method.
- `@ResponseStatus`: sets the HTTP status code of the response, instead of the default 200.
- `@Valid`: tells Spring to run the Bean Validation rules on this object (and the objects inside it) before going on.
- `@RequestBody`: fills this parameter by reading the JSON in the request body and converting it into the Java type.
- `@GetMapping`: routes HTTP GET requests for the given path to this method.
- `@PathVariable`: fills this parameter from the matching `{name}` part of the URL path.

Language features in this file, explained:

- **final**: `final` on a field or variable means it is assigned once and never changed; on a class it forbids subclasses.
- **private**: `private` hides a member from every other class.
- **public**: `public` lets any class use the member.


### `src/main/java/dev/plainround/orders/interfaces/web/CustomerResponse.java`

Package `dev.plainround.orders.interfaces.web`. A customer as the API shows it.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.interfaces.web;

import dev.plainround.orders.domain.Customer;

/**
 * A customer as the API shows it.
 *
 * @param id the identifier
 * @param name the display name
 */
public record CustomerResponse(long id, String name) {

    /**
     * Converts a domain customer.
     *
     * @param customer the customer
     * @return its API shape
     */
    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(customer.id(), customer.name());
    }
}
```

Reading it, one piece at a time:

- **`CustomerResponse`**, a record carrying `long id, String name`. A customer as the API shows it.
  - **method `from`**: Converts a domain customer.
    - Signature: `public static CustomerResponse from(Customer customer)`
    - input `customer`: the customer
    - gives back: its API shape
    - What the body does, in order:
      - returns `new CustomerResponse(customer.id(), customer.name())`

Language features in this file, explained:

- **record**: A `record` is a short way to declare a class that only carries data. The names in the parentheses become fields, a constructor, read methods with the same names, and `equals`, `hashCode` and `toString`.
- **static**: `static` means the member belongs to the class itself, not to one object; you use it without creating an object.
- **public**: `public` lets any class use the member.


### `src/main/java/dev/plainround/orders/interfaces/web/LineRequest.java`

Package `dev.plainround.orders.interfaces.web`. One line of a checkout request.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.interfaces.web;

import dev.plainround.orders.domain.OrderLine;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * One line of a checkout request.
 *
 * @param sku the product code
 * @param quantity the number of units
 */
public record LineRequest(
        @NotBlank(message = "sku must not be blank")
                @Size(max = OrderLine.MAX_SKU_LENGTH, message = "sku must be at most 64 characters")
                String sku,
        @Min(value = 1, message = "quantity must be at least 1")
                @Max(value = OrderLine.MAX_QUANTITY, message = "quantity must be at most 1000")
                int quantity) {

    /**
     * Converts to the domain line.
     *
     * @return the line
     */
    public OrderLine toDomain() {
        return new OrderLine(sku, quantity);
    }
}
```

Reading it, one piece at a time:

- **`LineRequest`**, a record carrying `@NotBlank(message = "sku must not be blank") @Size(max = OrderLine.MAX_SKU_LENGTH, message = "sku must be at most 64 characters") String sku, @Min(value = 1, message = "quantity must be at least 1") @Max(value = OrderLine.MAX_QUANTITY, message = "quantity must be at most 1000") int quantity`. One line of a checkout request.
  - **method `toDomain`**: Converts to the domain line.
    - Signature: `public OrderLine toDomain()`
    - gives back: the line
    - What the body does, in order:
      - returns `new OrderLine(sku, quantity)`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@NotBlank`: Bean Validation: the text must not be null and must contain at least one visible character.
- `@Size`: Bean Validation: the text or collection length must lie between the given bounds.
- `@Min`: Bean Validation: the number must be at least the given value.
- `@Max`: Bean Validation: the number must be at most the given value.

Language features in this file, explained:

- **record**: A `record` is a short way to declare a class that only carries data. The names in the parentheses become fields, a constructor, read methods with the same names, and `equals`, `hashCode` and `toString`.
- **public**: `public` lets any class use the member.


### `src/main/java/dev/plainround/orders/interfaces/web/NewCustomerRequest.java`

Package `dev.plainround.orders.interfaces.web`. The body of `POST /customers`.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.interfaces.web;

import dev.plainround.orders.domain.Customer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The body of {@code POST /customers}.
 *
 * @param name the display name
 */
public record NewCustomerRequest(
        @NotBlank(message = "name must not be blank")
                @Size(
                        max = Customer.MAX_NAME_LENGTH,
                        message = "name must be at most 100 characters")
                String name) {}
```

Reading it, one piece at a time:

- **`NewCustomerRequest`**, a record carrying `@NotBlank(message = "name must not be blank") @Size( max = Customer.MAX_NAME_LENGTH, message = "name must be at most 100 characters") String name`. The body of `POST /customers`.

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@NotBlank`: Bean Validation: the text must not be null and must contain at least one visible character.
- `@Size`: Bean Validation: the text or collection length must lie between the given bounds.

Language features in this file, explained:

- **record**: A `record` is a short way to declare a class that only carries data. The names in the parentheses become fields, a constructor, read methods with the same names, and `equals`, `hashCode` and `toString`.
- **public**: `public` lets any class use the member.


### `src/main/java/dev/plainround/orders/interfaces/web/OrderController.java`

Package `dev.plainround.orders.interfaces.web`. HTTP face of checkout and of a customer's order list.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.interfaces.web;

import dev.plainround.orders.application.CheckoutService;
import dev.plainround.orders.application.OrderQueryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** HTTP face of checkout and of a customer's order list. */
@RestController
@RequestMapping("/customers/{customerId}/orders")
public class OrderController {

    private final CheckoutService checkout;
    private final OrderQueryService queries;

    /**
     * Wires the controller.
     *
     * @param checkout the checkout use case
     * @param queries the order queries
     */
    public OrderController(CheckoutService checkout, OrderQueryService queries) {
        this.checkout = checkout;
        this.queries = queries;
    }

    /**
     * Places an order for the customer.
     *
     * @param customerId the buyer
     * @param request the validated body
     * @return the stored order
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse checkout(
            @PathVariable long customerId, @Valid @RequestBody CheckoutRequest request) {
        return OrderResponse.from(checkout.checkout(customerId, request.toDomain()));
    }

    /**
     * Lists the customer's orders with their lines.
     *
     * @param customerId the buyer
     * @return the orders, oldest first
     */
    @GetMapping
    public List<OrderResponse> list(@PathVariable long customerId) {
        return queries.listOrders(customerId).stream().map(OrderResponse::from).toList();
    }
}
```

Reading it, one piece at a time:

- **`OrderController`**, a class. HTTP face of checkout and of a customer's order list.
  - annotation `@RestController`: marks a class whose methods answer web requests, with return values written as JSON
  - annotation `@RequestMapping("/customers/{customerId}/orders")`: sets the URL path prefix for every handler method in this class (or the path and verb of one method)
  - **field `checkout`** of type `CheckoutService`, set once when the object is built.
    - Declaration: `private final CheckoutService checkout`
  - **field `queries`** of type `OrderQueryService`, set once when the object is built.
    - Declaration: `private final OrderQueryService queries`
  - **method `OrderController`**: Wires the controller.
    - Signature: `public OrderController(CheckoutService checkout, OrderQueryService queries)`
    - input `checkout`: the checkout use case
    - input `queries`: the order queries
    - What the body does, in order:
      - stores `checkout` in the field `checkout`
      - stores `queries` in the field `queries`
  - **method `checkout`**: Places an order for the customer.
    - Signature: `public OrderResponse checkout( @PathVariable long customerId, @Valid @RequestBody CheckoutRequest request)`
    - input `customerId`: the buyer
    - input `request`: the validated body
    - gives back: the stored order
    - annotation `@PostMapping`: routes HTTP POST requests for the given path to this method
    - annotation `@ResponseStatus(HttpStatus.CREATED)`: sets the HTTP status code of the response, instead of the default 200
    - What the body does, in order:
      - returns `OrderResponse.from(checkout.checkout(customerId, request.toDomain()))`
  - **method `list`**: Lists the customer's orders with their lines.
    - Signature: `public List<OrderResponse> list(@PathVariable long customerId)`
    - input `customerId`: the buyer
    - gives back: the orders, oldest first
    - annotation `@GetMapping`: routes HTTP GET requests for the given path to this method
    - What the body does, in order:
      - returns `queries.listOrders(customerId).stream().map(OrderResponse::from).toList()`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@RestController`: marks a class whose methods answer web requests, with return values written as JSON.
- `@RequestMapping`: sets the URL path prefix for every handler method in this class (or the path and verb of one method).
- `@PostMapping`: routes HTTP POST requests for the given path to this method.
- `@ResponseStatus`: sets the HTTP status code of the response, instead of the default 200.
- `@PathVariable`: fills this parameter from the matching `{name}` part of the URL path.
- `@Valid`: tells Spring to run the Bean Validation rules on this object (and the objects inside it) before going on.
- `@RequestBody`: fills this parameter by reading the JSON in the request body and converting it into the Java type.
- `@GetMapping`: routes HTTP GET requests for the given path to this method.

Language features in this file, explained:

- **final**: `final` on a field or variable means it is assigned once and never changed; on a class it forbids subclasses.
- **private**: `private` hides a member from every other class.
- **public**: `public` lets any class use the member.
- **method reference (`::`)**: `Type::method` is a short way to pass an existing method where a function is expected.
- **List**: A `List` is an ordered collection of items; `List.of(...)` builds one that cannot be changed afterwards.
- **Stream**: A stream processes a collection step by step: `map` converts each item, `filter` keeps some, `toList` collects the result.


### `src/main/java/dev/plainround/orders/interfaces/web/OrderResponse.java`

Package `dev.plainround.orders.interfaces.web`. An order as the API shows it.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.interfaces.web;

import dev.plainround.orders.domain.Order;
import dev.plainround.orders.domain.OrderLine;
import java.time.Instant;
import java.util.List;

/**
 * An order as the API shows it.
 *
 * @param id the identifier
 * @param customerId the buyer
 * @param placedAt when the order was placed
 * @param lines the lines
 */
public record OrderResponse(long id, long customerId, Instant placedAt, List<LineResponse> lines) {

    /**
     * One line of an order.
     *
     * @param sku the product code
     * @param quantity the number of units
     */
    public record LineResponse(String sku, int quantity) {

        static LineResponse from(OrderLine line) {
            return new LineResponse(line.sku(), line.quantity());
        }
    }

    /**
     * Converts a domain order.
     *
     * @param order the order
     * @return its API shape
     */
    public static OrderResponse from(Order order) {
        List<LineResponse> lines = order.lines().stream().map(LineResponse::from).toList();
        return new OrderResponse(order.id(), order.customerId(), order.placedAt(), lines);
    }
}
```

Reading it, one piece at a time:

- **`OrderResponse`**, a record carrying `long id, long customerId, Instant placedAt, List<LineResponse> lines`. An order as the API shows it.
  - **`LineResponse`**, a record carrying `String sku, int quantity`. One line of an order.
    - **method `from`**: the signature is `static LineResponse from(OrderLine line)`.
      - Signature: `static LineResponse from(OrderLine line)`
      - What the body does, in order:
        - returns `new LineResponse(line.sku(), line.quantity())`
  - **method `from`**: Converts a domain order.
    - Signature: `public static OrderResponse from(Order order)`
    - input `order`: the order
    - gives back: its API shape
    - What the body does, in order:
      - creates the variable `lines` (of type `List<LineResponse>`) holding `order.lines().stream().map(LineResponse::from).toList()`
      - returns `new OrderResponse(order.id(), order.customerId(), order.placedAt(), lines)`

Language features in this file, explained:

- **record**: A `record` is a short way to declare a class that only carries data. The names in the parentheses become fields, a constructor, read methods with the same names, and `equals`, `hashCode` and `toString`.
- **static**: `static` means the member belongs to the class itself, not to one object; you use it without creating an object.
- **public**: `public` lets any class use the member.
- **method reference (`::`)**: `Type::method` is a short way to pass an existing method where a function is expected.
- **List**: A `List` is an ordered collection of items; `List.of(...)` builds one that cannot be changed afterwards.
- **Stream**: A stream processes a collection step by step: `map` converts each item, `filter` keeps some, `toList` collects the result.


### `src/main/java/dev/plainround/orders/interfaces/web/OrdersExceptionHandler.java`

Package `dev.plainround.orders.interfaces.web`. The one place errors become `application/problem+json`: the service's named errors, validation failures with the failing fields, the framework's own exceptions (through the parent class) and a last resort that never leaks the cause.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.interfaces.web;

import dev.plainround.orders.domain.CustomerNotFound;
import dev.plainround.orders.domain.InvalidCustomer;
import dev.plainround.orders.domain.InvalidOrder;
import dev.plainround.orders.domain.OutOfStock;
import dev.plainround.orders.domain.StockConflict;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * The one place errors become {@code application/problem+json}: the service's named errors,
 * validation failures with the failing fields, the framework's own exceptions (through the parent
 * class) and a last resort that never leaks the cause.
 */
@RestControllerAdvice
public class OrdersExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(OrdersExceptionHandler.class);
    private static final String PROBLEM_URN = "urn:plainround:problem:";

    /**
     * Maps an unknown customer to 404.
     *
     * @param exception the error
     * @return the problem
     */
    @ExceptionHandler(CustomerNotFound.class)
    public ProblemDetail handleCustomerNotFound(CustomerNotFound exception) {
        return problem(
                HttpStatus.NOT_FOUND,
                "customer-not-found",
                "Customer not found",
                exception.getMessage());
    }

    /**
     * Maps a stock shortfall to 409; the checkout was rolled back as a whole.
     *
     * @param exception the error
     * @return the problem
     */
    @ExceptionHandler(OutOfStock.class)
    public ProblemDetail handleOutOfStock(OutOfStock exception) {
        return problem(HttpStatus.CONFLICT, "out-of-stock", "Out of stock", exception.getMessage());
    }

    /**
     * Maps a lost stock race to 409; retrying the checkout is safe because nothing was kept.
     *
     * @param exception the error
     * @return the problem
     */
    @ExceptionHandler(StockConflict.class)
    public ProblemDetail handleStockConflict(StockConflict exception) {
        return problem(
                HttpStatus.CONFLICT,
                "concurrent-update",
                "Concurrent update",
                exception.getMessage());
    }

    /**
     * Maps a rule break the field annotations cannot express (such as a repeated SKU) to 422.
     *
     * @param exception the error
     * @return the problem
     */
    @ExceptionHandler({InvalidOrder.class, InvalidCustomer.class})
    public ProblemDetail handleInvalid(RuntimeException exception) {
        return problem(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "invalid-order",
                "Invalid request",
                exception.getMessage());
    }

    /**
     * Maps anything unexpected to 500 without leaking its message.
     *
     * @param exception the error
     * @return the problem
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception) {
        LOG.error("unexpected error", exception);
        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "internal-error",
                "Internal error",
                "The request failed.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        List<Map<String, String>> errors =
                exception.getBindingResult().getFieldErrors().stream()
                        .map(OrdersExceptionHandler::fieldError)
                        .toList();
        return ResponseEntity.badRequest().body(validationProblem(errors));
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        List<Map<String, String>> errors =
                exception.getParameterValidationResults().stream()
                        .flatMap(result -> errorsOf(result).stream())
                        .toList();
        return ResponseEntity.badRequest().body(validationProblem(errors));
    }

    private static List<Map<String, String>> errorsOf(ParameterValidationResult result) {
        if (result instanceof ParameterErrors parameterErrors) {
            return parameterErrors.getFieldErrors().stream()
                    .map(OrdersExceptionHandler::fieldError)
                    .toList();
        }
        String parameter = String.valueOf(result.getMethodParameter().getParameterName());
        return result.getResolvableErrors().stream()
                .map(
                        error ->
                                Map.of(
                                        "field",
                                        parameter,
                                        "message",
                                        String.valueOf(error.getDefaultMessage())))
                .toList();
    }

    private static Map<String, String> fieldError(FieldError error) {
        return Map.of(
                "field", error.getField(), "message", String.valueOf(error.getDefaultMessage()));
    }

    private static ProblemDetail validationProblem(List<Map<String, String>> errors) {
        ProblemDetail problem =
                problem(
                        HttpStatus.BAD_REQUEST,
                        "validation-failed",
                        "Validation failed",
                        "The request failed validation.");
        problem.setProperty("errors", errors);
        return problem;
    }

    private static ProblemDetail problem(
            HttpStatus status, String name, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(PROBLEM_URN + name));
        problem.setTitle(title);
        return problem;
    }
}
```

Reading it, one piece at a time:

- **`OrdersExceptionHandler`**, a class that extends `ResponseEntityExceptionHandler`. The one place errors become `application/problem+json`: the service's named errors, validation failures with the failing fields, the framework's own exceptions (through the parent class) and a last resort that never leaks the cause.
  - annotation `@RestControllerAdvice`: marks a class that is shared by all controllers and turns exceptions into responses
  - **field `LOG`** of type `Logger`, starting as `LoggerFactory.getLogger(OrdersExceptionHandler.class)`
    - Declaration: `private static final Logger LOG = LoggerFactory.getLogger(OrdersExceptionHandler.class)`
  - **field `PROBLEM_URN`** of type `String`, starting as `"urn:plainround:problem:"`
    - Declaration: `private static final String PROBLEM_URN = "urn:plainround:problem:"`
  - **method `handleCustomerNotFound`**: Maps an unknown customer to 404.
    - Signature: `public ProblemDetail handleCustomerNotFound(CustomerNotFound exception)`
    - input `exception`: the error
    - gives back: the problem
    - annotation `@ExceptionHandler(CustomerNotFound.class)`: says this method handles the named exception type: when a controller throws it, Spring calls this method to build the response
    - What the body does, in order:
      - returns `problem( HttpStatus.NOT_FOUND, "customer-not-found", "Customer not found", exception.getMessage())`
  - **method `handleOutOfStock`**: Maps a stock shortfall to 409; the checkout was rolled back as a whole.
    - Signature: `public ProblemDetail handleOutOfStock(OutOfStock exception)`
    - input `exception`: the error
    - gives back: the problem
    - annotation `@ExceptionHandler(OutOfStock.class)`: says this method handles the named exception type: when a controller throws it, Spring calls this method to build the response
    - What the body does, in order:
      - returns `problem(HttpStatus.CONFLICT, "out-of-stock", "Out of stock", exception.getMessage())`
  - **method `handleStockConflict`**: Maps a lost stock race to 409; retrying the checkout is safe because nothing was kept.
    - Signature: `public ProblemDetail handleStockConflict(StockConflict exception)`
    - input `exception`: the error
    - gives back: the problem
    - annotation `@ExceptionHandler(StockConflict.class)`: says this method handles the named exception type: when a controller throws it, Spring calls this method to build the response
    - What the body does, in order:
      - returns `problem( HttpStatus.CONFLICT, "concurrent-update", "Concurrent update", exception.getMessage())`
  - **method `handleInvalid`**: Maps a rule break the field annotations cannot express (such as a repeated SKU) to 422.
    - Signature: `public ProblemDetail handleInvalid(RuntimeException exception)`
    - input `exception`: the error
    - gives back: the problem
    - annotation `@ExceptionHandler({InvalidOrder.class, InvalidCustomer.class})`: says this method handles the named exception type: when a controller throws it, Spring calls this method to build the response
    - What the body does, in order:
      - returns `problem( HttpStatus.UNPROCESSABLE_CONTENT, "invalid-order", "Invalid request", exception.getMessage())`
  - **method `handleUnexpected`**: Maps anything unexpected to 500 without leaking its message.
    - Signature: `public ProblemDetail handleUnexpected(Exception exception)`
    - input `exception`: the error
    - gives back: the problem
    - annotation `@ExceptionHandler(Exception.class)`: says this method handles the named exception type: when a controller throws it, Spring calls this method to build the response
    - What the body does, in order:
      - calls `LOG.error("unexpected error", exception)`
      - returns `problem( HttpStatus.INTERNAL_SERVER_ERROR, "internal-error", "Internal error", "The request failed.")`
  - **method `handleMethodArgumentNotValid`**: the signature is `ResponseEntity<Object> handleMethodArgumentNotValid( MethodArgumentNotValidException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request)`.
    - Signature: `protected ResponseEntity<Object> handleMethodArgumentNotValid( MethodArgumentNotValidException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request)`
    - annotation `@Override`: says this method replaces one with the same name from a parent class or interface; the compiler checks that such a parent method exists
    - What the body does, in order:
      - creates the variable `errors` (of type `List<Map<String, String>>`) holding `exception.getBindingResult().getFieldErrors().stream() .map(OrdersExceptionHandler::fieldError) .toList()`
      - returns `ResponseEntity.badRequest().body(validationProblem(errors))`
  - **method `handleHandlerMethodValidationException`**: the signature is `ResponseEntity<Object> handleHandlerMethodValidationException( HandlerMethodValidationException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request)`.
    - Signature: `protected ResponseEntity<Object> handleHandlerMethodValidationException( HandlerMethodValidationException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request)`
    - annotation `@Override`: says this method replaces one with the same name from a parent class or interface; the compiler checks that such a parent method exists
    - What the body does, in order:
      - creates the variable `errors` (of type `List<Map<String, String>>`) holding `exception.getParameterValidationResults().stream() .flatMap(result -> errorsOf(result).stream()) .toList()`
      - returns `ResponseEntity.badRequest().body(validationProblem(errors))`
  - **method `errorsOf`**: the signature is `static List<Map<String, String>> errorsOf(ParameterValidationResult result)`.
    - Signature: `private static List<Map<String, String>> errorsOf(ParameterValidationResult result)`
    - What the body does, in order:
      - when `result instanceof ParameterErrors parameterErrors` is true:
        - returns `parameterErrors.getFieldErrors().stream() .map(OrdersExceptionHandler::fieldError) .toList()`
      - creates the variable `parameter` (of type `String`) holding `String.valueOf(result.getMethodParameter().getParameterName())`
      - returns `result.getResolvableErrors().stream() .map( error -> Map.of( "field", parameter, "message", String.valueOf(error.getDefaultMessage()))) .toList()`
  - **method `fieldError`**: the signature is `static Map<String, String> fieldError(FieldError error)`.
    - Signature: `private static Map<String, String> fieldError(FieldError error)`
    - What the body does, in order:
      - returns `Map.of( "field", error.getField(), "message", String.valueOf(error.getDefaultMessage()))`
  - **method `validationProblem`**: the signature is `static ProblemDetail validationProblem(List<Map<String, String>> errors)`.
    - Signature: `private static ProblemDetail validationProblem(List<Map<String, String>> errors)`
    - What the body does, in order:
      - creates the variable `problem` (of type `ProblemDetail`) holding `problem( HttpStatus.BAD_REQUEST, "validation-failed", "Validation failed", "The request failed validation.")`
      - calls `problem.setProperty("errors", errors)`
      - returns `problem`
  - **method `problem`**: the signature is `static ProblemDetail problem( HttpStatus status, String name, String title, String detail)`.
    - Signature: `private static ProblemDetail problem( HttpStatus status, String name, String title, String detail)`
    - What the body does, in order:
      - creates the variable `problem` (of type `ProblemDetail`) holding `ProblemDetail.forStatusAndDetail(status, detail)`
      - calls `problem.setType(URI.create(PROBLEM_URN + name))`
      - calls `problem.setTitle(title)`
      - returns `problem`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@RestControllerAdvice`: marks a class that is shared by all controllers and turns exceptions into responses.
- `@ExceptionHandler`: says this method handles the named exception type: when a controller throws it, Spring calls this method to build the response.
- `@Override`: says this method replaces one with the same name from a parent class or interface; the compiler checks that such a parent method exists.

Language features in this file, explained:

- **final**: `final` on a field or variable means it is assigned once and never changed; on a class it forbids subclasses.
- **static**: `static` means the member belongs to the class itself, not to one object; you use it without creating an object.
- **private**: `private` hides a member from every other class.
- **public**: `public` lets any class use the member.
- **extends**: `extends X` makes the class a child of `X`: it inherits what `X` has and may replace parts.
- **lambda (`->`)**: An arrow `a -> b` is a lambda: a small unnamed function. The part before the arrow lists its inputs and the part after is what it does.
- **method reference (`::`)**: `Type::method` is a short way to pass an existing method where a function is expected.
- **List**: A `List` is an ordered collection of items; `List.of(...)` builds one that cannot be changed afterwards.
- **Map**: A `Map` stores values under keys, like a dictionary.
- **Stream**: A stream processes a collection step by step: `map` converts each item, `filter` keeps some, `toList` collects the result.


### `src/main/java/dev/plainround/orders/interfaces/web/RequestIdFilter.java`

Package `dev.plainround.orders.interfaces.web`. Gives every request an id for the logs: the caller's `X-Request-Id` when it looks safe, else a new one.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.interfaces.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gives every request an id for the logs: the caller's {@code X-Request-Id} when it looks safe,
 * else a new one. The id is in every JSON log line of the request ({@code requestId}) and in the
 * response header.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    /** Name of the request and response header. */
    public static final String HEADER = "X-Request-Id";

    private static final String MDC_KEY = "requestId";
    private static final Pattern SAFE = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = idFor(request.getHeader(HEADER));
        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private static String idFor(String incoming) {
        return incoming != null && SAFE.matcher(incoming).matches()
                ? incoming
                : UUID.randomUUID().toString();
    }
}
```

Reading it, one piece at a time:

- **`RequestIdFilter`**, a class that extends `OncePerRequestFilter`. Gives every request an id for the logs: the caller's `X-Request-Id` when it looks safe, else a new one. The id is in every JSON log line of the request (`requestId`) and in the response header.
  - annotation `@Component`: tells Spring to create one shared instance of this class at startup and manage it
  - annotation `@Order(Ordered.HIGHEST_PRECEDENCE)`: sets the sequence among several items of the same kind (a smaller number runs first)
  - **field `HEADER`** of type `String`: Name of the request and response header.
    - Declaration: `public static final String HEADER = "X-Request-Id"`
  - **field `MDC_KEY`** of type `String`, starting as `"requestId"`
    - Declaration: `private static final String MDC_KEY = "requestId"`
  - **field `SAFE`** of type `Pattern`, starting as `Pattern.compile("[A-Za-z0-9._-]{1,64}")`
    - Declaration: `private static final Pattern SAFE = Pattern.compile("[A-Za-z0-9._-]{1,64}")`
  - **method `doFilterInternal`**: the signature is `void doFilterInternal( HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException`.
    - Signature: `protected void doFilterInternal( HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException`
    - annotation `@Override`: says this method replaces one with the same name from a parent class or interface; the compiler checks that such a parent method exists
    - What the body does, in order:
      - creates the variable `requestId` (of type `String`) holding `idFor(request.getHeader(HEADER))`
      - calls `MDC.put(MDC_KEY, requestId)`
      - calls `response.setHeader(HEADER, requestId)`
      - tries this:
        - calls `chain.doFilter(request, response)`
      - whatever happened above, finally:
        - calls `MDC.remove(MDC_KEY)`
  - **method `idFor`**: the signature is `static String idFor(String incoming)`.
    - Signature: `private static String idFor(String incoming)`
    - What the body does, in order:
      - returns `incoming != null && SAFE.matcher(incoming).matches() ? incoming : UUID.randomUUID().toString()`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@Component`: tells Spring to create one shared instance of this class at startup and manage it.
- `@Order`: sets the sequence among several items of the same kind (a smaller number runs first).
- `@Override`: says this method replaces one with the same name from a parent class or interface; the compiler checks that such a parent method exists.

Language features in this file, explained:

- **final**: `final` on a field or variable means it is assigned once and never changed; on a class it forbids subclasses.
- **static**: `static` means the member belongs to the class itself, not to one object; you use it without creating an object.
- **private**: `private` hides a member from every other class.
- **public**: `public` lets any class use the member.
- **extends**: `extends X` makes the class a child of `X`: it inherits what `X` has and may replace parts.
- **throws**: `throws X` in a signature says the method may fail with exception `X`; an exception is an object that stops normal flow and travels up the call stack until something catches it.
- **null**: `null` means "no object". Calling a method on null throws `NullPointerException`.
- **try/catch**: `try { ... } catch (X e) { ... }` runs the first block and, if it throws `X`, runs the second.


### `src/main/java/dev/plainround/orders/interfaces/web/StockController.java`

Package `dev.plainround.orders.interfaces.web`. HTTP face of the stock levels.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.interfaces.web;

import dev.plainround.orders.application.StockService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HTTP face of the stock levels. */
@RestController
@RequestMapping("/stock/{sku}")
public class StockController {

    private final StockService service;

    /**
     * Wires the controller.
     *
     * @param service the stock use cases
     */
    public StockController(StockService service) {
        this.service = service;
    }

    /**
     * Sets the units on hand for a SKU.
     *
     * @param sku the product code
     * @param request the validated body
     * @return the new level
     */
    @PutMapping
    public StockResponse set(
            @PathVariable @NotBlank @Size(max = 64, message = "sku must be at most 64 characters")
                    String sku,
            @Valid @RequestBody StockRequest request) {
        service.setAvailable(sku, request.available());
        return new StockResponse(sku, request.available());
    }

    /**
     * Reports the units on hand for a SKU.
     *
     * @param sku the product code
     * @return the level
     */
    @GetMapping
    public StockResponse get(@PathVariable String sku) {
        return new StockResponse(sku, service.available(sku));
    }
}
```

Reading it, one piece at a time:

- **`StockController`**, a class. HTTP face of the stock levels.
  - annotation `@RestController`: marks a class whose methods answer web requests, with return values written as JSON
  - annotation `@RequestMapping("/stock/{sku}")`: sets the URL path prefix for every handler method in this class (or the path and verb of one method)
  - **field `service`** of type `StockService`, set once when the object is built.
    - Declaration: `private final StockService service`
  - **method `StockController`**: Wires the controller.
    - Signature: `public StockController(StockService service)`
    - input `service`: the stock use cases
    - What the body does, in order:
      - stores `service` in the field `service`
  - **method `set`**: Sets the units on hand for a SKU.
    - Signature: `public StockResponse set( @PathVariable @NotBlank @Size(max = 64, message = "sku must be at most 64 characters") String sku, @Valid @RequestBody StockRequest request)`
    - input `sku`: the product code
    - input `request`: the validated body
    - gives back: the new level
    - annotation `@PutMapping`: routes HTTP PUT requests for the given path to this method
    - What the body does, in order:
      - calls `service.setAvailable(sku, request.available())`
      - returns `new StockResponse(sku, request.available())`
  - **method `get`**: Reports the units on hand for a SKU.
    - Signature: `public StockResponse get(@PathVariable String sku)`
    - input `sku`: the product code
    - gives back: the level
    - annotation `@GetMapping`: routes HTTP GET requests for the given path to this method
    - What the body does, in order:
      - returns `new StockResponse(sku, service.available(sku))`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@RestController`: marks a class whose methods answer web requests, with return values written as JSON.
- `@RequestMapping`: sets the URL path prefix for every handler method in this class (or the path and verb of one method).
- `@PutMapping`: routes HTTP PUT requests for the given path to this method.
- `@PathVariable`: fills this parameter from the matching `{name}` part of the URL path.
- `@NotBlank`: Bean Validation: the text must not be null and must contain at least one visible character.
- `@Size`: Bean Validation: the text or collection length must lie between the given bounds.
- `@Valid`: tells Spring to run the Bean Validation rules on this object (and the objects inside it) before going on.
- `@RequestBody`: fills this parameter by reading the JSON in the request body and converting it into the Java type.
- `@GetMapping`: routes HTTP GET requests for the given path to this method.

Language features in this file, explained:

- **final**: `final` on a field or variable means it is assigned once and never changed; on a class it forbids subclasses.
- **private**: `private` hides a member from every other class.
- **public**: `public` lets any class use the member.


### `src/main/java/dev/plainround/orders/interfaces/web/StockRequest.java`

Package `dev.plainround.orders.interfaces.web`. The body of `PUT /stock/{sku`}.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.interfaces.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * The body of {@code PUT /stock/{sku}}.
 *
 * @param available the units on hand
 */
public record StockRequest(
        @Min(value = 0, message = "available must be 0 or more")
                @Max(value = 1_000_000, message = "available must be at most 1000000")
                int available) {}
```

Reading it, one piece at a time:

- **`StockRequest`**, a record carrying `@Min(value = 0, message = "available must be 0 or more") @Max(value = 1_000_000, message = "available must be at most 1000000") int available`. The body of `PUT /stock/{sku`}.

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@Min`: Bean Validation: the number must be at least the given value.
- `@Max`: Bean Validation: the number must be at most the given value.

Language features in this file, explained:

- **record**: A `record` is a short way to declare a class that only carries data. The names in the parentheses become fields, a constructor, read methods with the same names, and `equals`, `hashCode` and `toString`.
- **public**: `public` lets any class use the member.


### `src/main/java/dev/plainround/orders/interfaces/web/StockResponse.java`

Package `dev.plainround.orders.interfaces.web`. The units on hand for a SKU.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.interfaces.web;

/**
 * The units on hand for a SKU.
 *
 * @param sku the product code
 * @param available the units
 */
public record StockResponse(String sku, int available) {}
```

Reading it, one piece at a time:

- **`StockResponse`**, a record carrying `String sku, int available`. The units on hand for a SKU.

Language features in this file, explained:

- **record**: A `record` is a short way to declare a class that only carries data. The names in the parentheses become fields, a constructor, read methods with the same names, and `equals`, `hashCode` and `toString`.
- **public**: `public` lets any class use the member.


## Try it yourself

1. Open the file that has `main` in it and find the `SpringApplication.run` call. Everything else is reached from there.
2. Pick one class and read its Javadoc (the `/** ... */` comment above it) first. It states the job in one sentence.
3. Run `./mvnw -q compile` and look in `target/classes`: each `.java` file became a `.class` file of the same name.

## Mistakes people make

- Reading the methods in file order instead of following a request. Follow one request from the controller down.
- Thinking an annotation does nothing. Each `@` label above is read by Spring or by a validation library, and removing one changes behavior.
- Mixing layers: a domain class must not import Spring. The architecture test in [ArchUnit](archunit.md) fails the build if it does.

## Check yourself

1. Which folder holds the classes that know nothing about HTTP or databases? *The domain folder.*
2. Who creates the objects the constructors ask for? *Spring, at startup.*
3. What does `final` on a field promise? *It is set once, in the constructor.*

## Further reading

[Java](java.md), [Spring Framework](spring-framework.md), [Spring Boot](spring-boot.md), [Spring Web MVC](spring-web-mvc.md) and [the test code note](java-test-code.md) in this folder.

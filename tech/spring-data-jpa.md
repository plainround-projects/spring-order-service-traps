# JPA, Hibernate and Spring Data JPA

## What it is

- **JPA** (Jakarta Persistence) is a Java standard for mapping objects to database tables. A class marked `@Entity` stands for a table; its fields stand for columns; an object stands for a row.
- **Hibernate** is the library that implements JPA. It tracks the objects you load, notices when you change them, and writes the SQL.
- **Spring Data JPA** lets you declare a repository as an interface (`interface OrderJpaRepository extends JpaRepository<OrderEntity, Long>`) and writes the implementation for you, including simple queries from method names.

## Why this repository uses it

The whole lesson is the traps of JPA: lazy loading (the N+1 query problem), the transaction proxy, and the audit record. You cannot understand them without seeing how entities, sessions and lazy collections work.

## Where it appears

| Path | Role |
| --- | --- |
| `infrastructure/persistence/*Entity.java` | the mapped classes: `CustomerEntity`, `OrderEntity`, `OrderLineEntity`, `StockItemEntity`, `AuditRecordEntity` |
| `infrastructure/persistence/*JpaRepository.java` | the Spring Data interfaces |
| `infrastructure/persistence/Jpa*Repository.java`, `JpaAuditLog.java` | adapters that implement the domain's interfaces and convert entities to domain records |
| `src/test/java/.../traps/` | tests that count statements and show the lazy-loading and open-in-view behavior |

## How it works underneath

1. A **persistence context** (also called the session, or the `EntityManager`) is a cache of the entities loaded or saved in one unit of work, normally one transaction. Loading the same row twice in a transaction gives the same object.
2. **Dirty checking:** when the transaction commits (or when something forces a **flush**), Hibernate compares each loaded entity with its original state and writes `UPDATE` statements for the ones that changed. You do not call `update`.
3. **Lazy loading:** an association marked `fetch = LAZY` is not loaded with its owner. Hibernate puts a placeholder (a **proxy** or a special collection) that loads the data from the database the first time you touch it. That needs an open session. Outside a transaction it throws `LazyInitializationException`.
4. **The N+1 problem:** load 20 orders (1 query), then touch each order's lines: 20 more queries. Total 21. A **fetch join** or an **entity graph** loads the lines in the same query.
5. **Open Session In View** (OSIV) keeps the session open for the whole web request, so lazy loading works in the controller. Spring Boot turns it on by default. It hides lazy-loading bugs, so this project sets `spring.jpa.open-in-view=false`.
6. **Optimistic locking:** a `@Version` field is checked in every `UPDATE ... WHERE id = ? AND version = ?`. If another transaction changed the row first, no row matches and Hibernate throws an exception.
7. Hibernate keeps **statistics** (statements prepared, entities loaded) when `hibernate.generate_statistics=true`; the tests turn it on to count statements.

## Walkthrough of this repository's use

**An entity.**

```java
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
```

`@Entity` and `@Table(name = "orders_table")` map the class to the table. `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)` says the id column is assigned by the database (the identity column). `@ManyToOne(fetch = FetchType.LAZY)` with `@JoinColumn(name = "customer_id")` maps the foreign key; a `@ManyToOne` is eager by default, so `LAZY` is written down. `@OneToMany(mappedBy = "order", cascade = CascadeType.ALL)` says the lines belong to the order: `mappedBy` points to the field on the other side that owns the foreign key, and `cascade = ALL` means saving or deleting the order does the same to its lines. `@OrderBy("id")` sorts the lines.

**The optimistic lock.**

```java
@Entity
@Table(name = "stock_items")
public class StockItemEntity {

    @Id private String sku;

    private int available;

    @Version private long version;
```

`@Version` makes Hibernate add the version check to every update and increment it.

**A repository interface.**

```java
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

Extending `JpaRepository<OrderEntity, Long>` gives `save`, `findById`, `count` and more with no code. The `@Query` is JPQL, a query language over entities: `select o from OrderEntity o where o.customer.id = :customerId order by o.id`. `@EntityGraph(attributePaths = "lines")` tells Hibernate to load the `lines` collection in the same SQL statement with a join. That is the **fix for the first trap**: one statement however many orders.

**The naive version, in test code, for comparison.**

```java
public interface NaiveOrderJpaRepository extends JpaRepository<OrderEntity, Long> {

    /**
     * One query for the orders; every access to {@code getLines()} afterwards costs one more.
     *
     * @param customerId the buyer
     * @return the orders with lazy lines
     */
    List<OrderEntity> findByCustomerId(Long customerId);
}
```

A derived query: Spring Data builds `select ... where customer_id = ?` from the method name `findByCustomerId`. Lines load lazily, one query per order.

**The adapter** converts entities to domain records inside the transaction:

```java
public Order place(OrderDraft draft, Instant placedAt) {
    OrderEntity entity = new OrderEntity(placedAt);
    draft.lines()
            .forEach(line -> entity.addLine(new OrderLineEntity(line.sku(), line.quantity())));
    entity.setCustomer(customers.getReferenceById(draft.customerId()));
    return toDomain(orders.save(entity));
}
```

`new OrderEntity(placedAt)`, `entity.addLine(...)` for each line, `entity.setCustomer(customers.getReferenceById(...))` (a reference without a query), `orders.save(entity)` stores the order and, by cascade, all its lines.

**Optimistic locking in the stock adapter.**

```java
public void reserve(String sku, int quantity) {
    StockItemEntity item = stock.findById(sku).orElseThrow(() -> new OutOfStock(sku));
    if (item.getAvailable() < quantity) {
        throw new OutOfStock(sku);
    }
    item.setAvailable(item.getAvailable() - quantity);
    flushOrConflict(item, sku);
}
```

It loads the stock row, refuses if there are too few units, decreases the number, and `saveAndFlush` forces the `UPDATE` now. If another checkout changed the row, Hibernate raises an optimistic-lock exception, which the adapter turns into the domain's `StockConflict`.

**The counting tests.** `NPlusOneIT` reads `entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics()` and asserts `getPrepareStatementCount()`: 21 for the naive listing of 20 orders, 201 for 200, and 1 for the entity-graph version at both sizes. `OpenInViewIT` and `OpenInViewOnIT` show the lazy load failing (HTTP 500) when open-in-view is off and being hidden (HTTP 200) when it is on.

## Try it yourself

1. `make traps` runs the trap tests and prints the measured numbers (needs Docker).
2. Run `NPlusOneIT` alone and read the statement counts it asserts.
3. In a scratch copy, remove `@EntityGraph` from `findByCustomerIdWithLines` and run `NPlusOneIT`: the fixed-listing tests fail with a count above 2.
4. Set `spring.jpa.show-sql=true` for one run to see the SQL Hibernate sends.

## Mistakes people make

- Touching a lazy collection outside a transaction.
- Returning entities from controllers.
- Leaving open-in-view on and never noticing N+1 or missing fetches.
- Calling `save` on every change: managed entities are written at commit anyway.
- Applying `cascade = ALL` in both directions without thinking about who owns the lifecycle.

## Check yourself

1. What is N+1? *One query for a list plus one more per item for a lazy association.*
2. What does `fetch = LAZY` mean? *Load the association on first access.*
3. What does `@Version` protect against? *Two transactions overwriting each other's update.*
4. What does an entity graph do? *Loads named associations in the same query.*
5. Why is open-in-view off here? *To make lazy-loading bugs fail where they can be seen.*

## Further reading

- Spring Data JPA (reference for the Spring Data release train 2026.0): https://docs.spring.io/spring-data/jpa/reference/
- Hibernate ORM 7 user guide: https://docs.hibernate.org/orm/7.0/userguide/html_single/Hibernate_User_Guide.html
- Jakarta Persistence 3.2: https://jakarta.ee/specifications/persistence/3.2/

# Java 25

## What it is

Java is a programming language. You write source code in `.java` files. A tool called the **compiler** (`javac`) turns each file into **bytecode** (`.class` files). A program called the **JVM** (Java Virtual Machine, started by the `java` command) reads the bytecode and runs it. A **JDK** (Java Development Kit) is the compiler, the JVM and the standard library in one download. A **JRE** is the JVM and the standard library without the compiler.

## Why this repository uses it

The lesson is about Spring, and Spring is a Java framework. This repository is written for Java 25: the lesson names no version, so the repository uses the current long-term-support release.

## Where it appears

| Path | What it does with Java |
| --- | --- |
| `src/main/java/dev/plainround/orders/domain/` | records for the business data (`Order`, `OrderLine`, `OrderDraft`), the named errors and the interfaces ("ports") the application needs |
| `src/main/java/.../application/` | the use cases, `CheckoutService` first among them |
| `src/main/java/.../infrastructure/persistence/` | the database entities and the classes that talk to them |
| `src/main/java/.../interfaces/web/` | the HTTP controllers |
| `src/test/java/.../traps/` | the three traps, written as tests |

## How it works underneath

1. `javac` reads `.java` files and writes `.class` files. In this repository Maven calls it for you.
2. A **package** is a folder of classes. The line `package dev.plainround.orders.domain;` at the top of a file says which folder the class belongs to.
3. A **class** describes a kind of object: its data (fields) and its actions (methods). A **record** is a short form of a class that only holds data and cannot change after it is built. An **interface** lists methods without writing their code; other classes promise to provide them.
4. At run time the JVM loads classes when they are first needed, runs `public static void main(String[] args)` of the class you name, and frees memory that nothing points to any more (**garbage collection**).
5. An **annotation** such as `@Override` or `@Test` is a label written in front of a class or method. The compiler, a test runner or a framework reads the label and acts on it.
6. A **lambda** such as `line -> line.sku()` is a small function written inline. A **stream** is a pipeline that applies lambdas to a list: `list.stream().map(...).toList()`.

## Walkthrough of this repository's use

**Records.** A record declares its fields in the header and gets the constructor, the accessors and `equals`, `hashCode` and `toString` for free.

```java
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
`public record OrderLine(String sku, int quantity)` declares two fields and gives the accessors `sku()` and `quantity()`. The compact constructor `public OrderLine { ... }` checks the rules (a SKU of 1 to 64 characters, a quantity of 1 to 1000) and throws `InvalidOrder` if they are broken. A record cannot be built in a broken state, so every part of the program can trust one.

**Sealed classes and `switch` patterns.** Not used in this repository: sealed classes and `switch` patterns. The code keeps to records, interfaces, lambdas and streams.

**Interfaces as ports.** `OrderRepository`, `StockRepository` and `AuditLog` are interfaces in `domain`; the classes in `infrastructure.persistence` implement them. The use cases depend on the interfaces only.

**Lambdas and streams.** `JpaOrderRepository.place` uses `draft.lines().forEach(line -> entity.addLine(...))` and `findByCustomerId` uses `stream().map(JpaOrderRepository::toDomain).toList()`.

**Checked and unchecked exceptions.** All the project's errors extend `RuntimeException` (unchecked): the compiler does not force every caller to catch them. Each has a name that says what went wrong, such as `OutOfStock`, and one place (the "advice" class) turns them into HTTP answers.

**Compiler settings.** The build runs `javac` with `-Xlint:all -Werror`: every warning becomes an error. That keeps unchecked casts and deprecated calls out of the code.

## Try it yourself

1. `java -version` prints the JDK you have. This repository needs 25 or newer.
2. `./mvnw -q compile` compiles the code. Afterwards `ls target/classes` shows the `.class` files in the same folders as the packages.
3. `javap -cp target/classes dev.plainround.orders.domain.OrderLine` (from ``) prints the methods the compiler made for the record: the accessors, `equals`, `hashCode`, `toString`.
4. `jshell` starts an interactive prompt; type `record Point(int x, int y) {}` then `new Point(1,2)` to see a record's `toString`.

## Mistakes people make

- Changing a record's data: it cannot be done. Build a new record instead, as `markedRead()` does.
- Using `==` on strings: it compares identity, not content. Use `.equals`.
- Catching `Exception` everywhere: it hides bugs. This repository catches one named type where it can act on it, and the advice class handles the rest in one place.
- Building with an older JDK than the code needs: `./mvnw` says "release version 25 not supported". Install a newer JDK.
- Long methods. The house style here is short methods with names that say what they do.

## Check yourself

1. What is the difference between the JDK and the JRE? *The JDK also has the compiler.*
2. What does a record's compact constructor do? *Runs before the fields are set; used to check values.*
3. Why does `markedRead()` return a new `Book`? *Records cannot change after they are built.*
4. What does `-Werror` do to a compiler warning? *Turns it into an error that fails the build.*
5. What is an interface for, in this repository? *It names what a layer needs (a "port"), so another class can provide it.*

## Further reading

- The Java Language Specification and the `java.base` API for Java 25: https://docs.oracle.com/en/java/javase/25/
- Records: https://docs.oracle.com/en/java/javase/25/language/records.html

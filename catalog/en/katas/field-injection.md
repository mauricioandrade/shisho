---
id: field-injection
---

## Why

The Spring team recommends constructor injection: the object is born complete, required
dependencies are never `null` and the fields can be `final`. With `@Autowired` on a field,
`new OrderService()` compiles and creates an invalid object, which only breaks with a
`NullPointerException` when someone calls a method.

The constructor also documents the class. Whoever reads its signature knows what it needs, and a
test builds the class with `new OrderService(fake)`, with no Spring and no reflection. It also
makes the cost visible: a constructor with eight parameters is uncomfortable, and that discomfort
warns that the class has too many responsibilities (the Spring documentation calls it a code
smell).

The argument for field injection is writing less. True, but what it saves is precisely the part
that documents the class.

## Acceptable when

In test classes with `@SpringBootTest`, where the framework creates the instance and nobody builds
it by hand, `@Autowired` on fields is common and appears in the Spring documentation itself (the
Shishō does not analyze `src/test`). In legacy code, migrating gradually is worth more than
rewriting everything at once. Optional dependencies may come through a setter, as the Spring
documentation suggests, but that is rare in a Spring Boot application.

In a Quarkus project, `@Inject` on a package-private field is the style its own documentation
recommends: ArC generates, at build time, the code that fills the field, with no reflection. The
Shishō recognizes Quarkus by the build (an `io.quarkus` dependency) and, there, only flags
`@Autowired`.

## Before

```java
@Service
public class OrderService {

    @Autowired
    private OrderRepository orders;
}
```

## After

The constructor declares the dependency; `final` guarantees it does not change. With a single
constructor, Spring injects without `@Autowired`, and the test creates the class with
`new OrderService(fake)`.

```java
@Service
public class OrderService {

    private final OrderRepository orders;

    public OrderService(OrderRepository orders) {
        this.orders = orders;
    }
}
```

---
id: lombok-constructor
---

## Why

Using Lombok is not wrong, and many serious projects use it. The question is the cost. Lombok is
not an ordinary library: it is an annotation processor that changes javac's internal syntax
tree, an API the JDK neither publishes nor promises to keep. That is why every new Java release
needs a new Lombok release (JDK 25 support arrived in 1.18.40), and upgrading Java comes to
depend on another project's schedule.

In the constructor's case, the hidden code is the most important part of the class: the list of
what it needs to exist. Hiding it has subtle effects. The `@Qualifier` (or `@Value`, `@Lazy`) you
put on the field is not copied to the constructor parameter unless you set
`lombok.copyableAnnotations` in a `lombok.config`, and Spring injects the wrong bean or fails
without the code showing why.

The gain, today, is small. With a single constructor, Spring injects without `@Autowired`, the
IDE generates the constructor with a shortcut and records cover the data cases. What is left is a
short constructor, written once.

## Acceptable when

In a project that already uses Lombok consistently, with the team used to it and `lombok.config`
set up, switching may not be worth the effort. If you keep it, prefer `@RequiredArgsConstructor`
over `@AllArgsConstructor`, which puts every field in the constructor, including the ones that
are not dependencies. If you switch, `shisho fix` writes the constructor for you.

The kata only looks at beans (`@Service`, `@Component`, `@ApplicationScoped`...). In a data class
with `@Data` and `@AllArgsConstructor`, the constructor holds data, not dependencies; there the
question is whether it should be a record, and `dto-not-record` is the one asking it.

## Before

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orders;
    private final Clock clock;
}
```

## After

```java
@Service
public class OrderService {

    private final OrderRepository orders;
    private final Clock clock;

    public OrderService(OrderRepository orders, Clock clock) {
        this.orders = orders;
        this.clock = clock;
    }
}
```

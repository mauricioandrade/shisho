---
id: quarkus-private-injection
---

## Why

Quarkus sets up dependency injection at build time, not at startup. ArC, its CDI
implementation, reads the beans during the build and generates Java classes that create each one
and assign the fields directly, as if someone had written `service.orders = orders`. Those classes
live in the bean's package, so they see package-private fields, but not `private` ones.

For a `private` field, ArC falls back to reflection: more work at startup and, in a native image
(GraalVM), the field must be registered for reflection. Quarkus registers it on its own and warns
in the log ("unrecommended usage of private members"), but the cost is still there. Dropping
`private` is the whole fix.

In a Spring project the rule is different, and `field-injection` takes care of it: there the
recommendation is the constructor.

## Acceptable when

In a class you cannot change, or when the field's encapsulation matters more than a few
milliseconds of startup in an application that runs on the JVM. Constructor injection avoids the
problem too, and with it the field can be `private final`.

## Before

```java
@ApplicationScoped
public class OrderService {

    @Inject
    private OrderRepository orders;
}
```

## After

Package-private: the code ArC generates assigns the field without reflection.

```java
@ApplicationScoped
public class OrderService {

    @Inject
    OrderRepository orders;
}
```

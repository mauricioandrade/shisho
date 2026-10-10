---
id: non-final-dependency
---

## Why

With `final`, the compiler guarantees two things: the dependency is assigned exactly once, in the
constructor, and nobody replaces it afterwards. Without `final`, a setter or a forgotten method
can swap the repository halfway through the life of the bean, which is a singleton shared by
every request.

There is also a concurrency guarantee. The Java memory model ensures that any thread sees `final`
fields already initialized once the constructor finishes (JLS 17.5). In a singleton used by
several threads at once, that is one worry less.

## Acceptable when

Almost never, if the field comes from the constructor. When a dependency really must change after
construction, as in a component reconfigured at runtime through JMX (the example the Spring
documentation gives for setter injection), it does not come through the constructor, and this
kata does not flag it.

## Before

```java
@Service
public class OrderService {

    private OrderRepository orders;

    public OrderService(OrderRepository orders) {
        this.orders = orders;
    }
}
```

## After

```java
@Service
public class OrderService {

    private final OrderRepository orders;

    public OrderService(OrderRepository orders) {
        this.orders = orders;
    }
}
```

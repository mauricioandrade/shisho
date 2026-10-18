---
id: unproxyable-method
---

## Why

`@Transactional`, `@Async` and `@Cacheable` do nothing on their own. Spring creates a **proxy**, an
object that stands in front of the bean, and the proxy is what opens the transaction, hands the
call to another thread or checks the cache before calling the real method. Whoever injects the bean
gets the proxy.

The proxy is a subclass generated at runtime, and a subclass can only intercept what it can
override. A `private` method is invisible to it, a `static` one does not belong to the object and a
`final` one cannot be overridden. In all three cases the annotation is ignored **without any
warning**: the code compiles, starts and runs without a transaction. The bug only shows when a
failure halfway through the method leaves half of the data written.

In Spring Boot 2 (Spring 5) the rule is narrower: the proxy only intercepts `public` methods.
Spring 6 started accepting `protected` and package-private methods in class-based proxies.

## Acceptable when

Never: the annotation has no effect on that method, so either the annotation goes or the method
changes. The same trap appears when one method of the bean calls another annotated method of the
**same class** (`this.save()`): the call does not go through the proxy. The kata does not see that
case, because the model does not say which method each call comes from; the way out is the same,
moving the annotated method to another bean.

The kata does not run on Quarkus: since Quarkus 3, an interceptor annotation on a `private` method
fails the build, and ArC intercepts `static` methods.

## Before

```java
@Service
public class OrderService {

    public void close(Order order) {
        saveItems(order);
    }

    @Transactional
    private void saveItems(Order order) {
        order.items().forEach(items::save);
    }
}
```

## After

```java
@Service
public class OrderService {

    @Transactional
    public void close(Order order) {
        saveItems(order);
    }

    private void saveItems(Order order) {
        order.items().forEach(items::save);
    }
}
```

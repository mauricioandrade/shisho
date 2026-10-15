---
id: system-out
---

## Why

`System.out` has no level: you cannot silence the detail in production, or turn on `debug` for a
single package while investigating a problem. It has no format: it comes out without a
timestamp, thread or source class, and without the structured (JSON) output a log aggregator
indexes. And it always writes to the same place.

`printStackTrace()` is worse. The stack trace goes to `System.err`, apart from the message and
outside the log, and it usually sits in a `catch` that swallows the exception and lets the system
carry on in an inconsistent state.

Spring Boot already ships SLF4J and Logback configured. Outside it, the JDK itself has had
`System.Logger` since Java 9 (JEP 264), a facade that forwards to whatever logging framework is
present.

In Quarkus, the shortcut is `io.quarkus.logging.Log`: `Log.info("Order created")` directly, with no
logger declared per class. At build time, Quarkus replaces each call with a logger of the calling
class, so the log still says where it came from.

## Acceptable when

When the console is the interface: the `main` of a command line tool (the Shishō itself writes
its report to the console), scripts and teaching examples. There the output is the product, not
a diagnostic. In a server application, it is not.

## Before

```java
@Service
public class OrderService {

    public void create(Order order) {
        try {
            System.out.println("creating order " + order.id());
            save(order);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## After

The logger has a level (you can silence `debug` in production), a configurable format and
destination, and the exception goes whole into the log, together with the message.

```java
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    public void create(Order order) {
        log.debug("creating order {}", order.id());
        try {
            save(order);
        } catch (DataAccessException e) {
            log.error("failed to save order {}", order.id(), e);
            throw e;
        }
    }
}
```

## Before (Quarkus)

```java
@ApplicationScoped
public class OrderService {

    public void create(Order order) {
        try {
            System.out.println("creating order " + order.id());
            save(order);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## After (Quarkus)

`Log`, from `io.quarkus.logging`, saves you declaring the logger: at build time, Quarkus
replaces each call with a logger of the calling class. Level, format and destination stay
configurable in `quarkus.log.*`.

```java
@ApplicationScoped
public class OrderService {

    public void create(Order order) {
        Log.debugf("creating order %s", order.id());
        try {
            save(order);
        } catch (PersistenceException e) {
            Log.errorf(e, "failed to save order %s", order.id());
            throw e;
        }
    }
}
```

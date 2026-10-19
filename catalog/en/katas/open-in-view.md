---
id: open-in-view
---

## Why

With Spring Data JPA, Spring Boot turns on **Open Session in View** (OSIV) by itself: the
`EntityManager` opens at the start of the request and only closes when the response leaves. The
intent is good. A `LAZY` relationship read in the controller, or by Jackson while building the
JSON, works instead of throwing `LazyInitializationException`.

The price shows under load. Every request holds a connection from the pool for the whole handling,
including while it calls other services or writes the response, and the pool runs out much sooner
than it should. The queries OSIV allows happen outside the service, with no transaction, one per
relationship read (the N+1), and hidden in serialization, where nobody looks.

The debate is old enough that Spring Boot itself warns in the log, on every start, when nobody
declared the property: `spring.jpa.open-in-view is enabled by default`.

## Acceptable when

When the project chose OSIV, knowing the cost: an application with Thymeleaf pages and little
traffic, for instance. Just declare `spring.jpa.open-in-view=true` and the Shishō accepts it,
because the choice is written down. The kata also leaves alone a module that imports its settings
from a config server, since the property may be there.

## Before

```properties
spring.datasource.url=jdbc:postgresql://db:5432/shop
```

## After

With OSIV off, the service loads what the response needs (`JOIN FETCH`, `@EntityGraph` or a DTO
projection), and a forgotten lazy access fails in the test instead of becoming a hidden query in
production.

```properties
spring.datasource.url=jdbc:postgresql://db:5432/shop
spring.jpa.open-in-view=false
```

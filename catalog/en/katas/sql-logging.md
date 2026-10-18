---
id: sql-logging
---

## Why

Seeing the SQL Hibernate generates is one of the best ways to learn JPA and to find an N+1. That
is why `spring.jpa.show-sql=true` shows up in so many tutorials. The problem is when it goes to
production along with everything else.

In production, every request becomes several log lines, and logs cost money: on disk, in the
aggregator (Datadog, CloudWatch, Loki) and in the time of whoever looks for an error in the noise.
`show-sql` writes to `System.out`, outside the logging system, with no level and no format. And,
with the parameter logger on, the values go along: e-mails, tax IDs, whatever is in the query.

## Acceptable when

In a development profile (`application-dev.properties`, `%dev.` in Quarkus), which the Shishō does
not flag. To investigate a query in production, prefer turning the `org.hibernate.SQL` logger on
for a while (Spring's actuator changes the level at runtime, at `/actuator/loggers`) or looking at
Hibernate's metrics and slow query log.

## Before

```properties
spring.jpa.show-sql=true
```

## After

The SQL is still visible in development: the same key, in `application-dev.properties`.

```properties
spring.jpa.show-sql=false
```

## Before (Quarkus)

```properties
quarkus.hibernate-orm.log.sql=true
```

## After (Quarkus)

```properties
%dev.quarkus.hibernate-orm.log.sql=true
```

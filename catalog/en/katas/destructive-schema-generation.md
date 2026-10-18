---
id: destructive-schema-generation
---

## Why

Hibernate can create the tables from the entities, and that is great for getting started: with
`ddl-auto=create-drop` (or `create`, or `drop-and-create` in Quarkus), every start of the
application drops the schema and creates it again, empty. In development and in tests, that is
exactly what you want. In production, it means dropping the database on every deploy, every
container restart, every time Kubernetes moves the pod to another machine.

The danger is the setting leaking where it should not be. In Spring, an `application.properties`
applies to every profile. Quarkus is trickier: dev mode already generates the schema on its own,
and a key without a prefix, which looks like "the development setting", applies to production too.

In production, the schema is code: it changes through versioned migrations (Flyway, Liquibase),
reviewed and applied once, and Hibernate only checks it (`validate`) or leaves it alone (`none`).

## Acceptable when

For a database that is born and dies with the application: an in-memory H2 in a demo, a test
container. Spring Boot already uses `create-drop` by itself for an embedded database, without
anyone writing anything. If the setting is for development only, put it in an
`application-dev.properties` (or behind the `%dev.` prefix in Quarkus), and the Shishō stops
flagging it.

## Before

```properties
spring.datasource.url=jdbc:postgresql://db:5432/shop
spring.jpa.hibernate.ddl-auto=create-drop
```

## After

The schema changes through Flyway migrations (`src/main/resources/db/migration/V1__orders.sql`),
and Hibernate only checks that the entities match it.

```properties
spring.datasource.url=jdbc:postgresql://db:5432/shop
spring.jpa.hibernate.ddl-auto=validate
```

## Before (Quarkus)

```properties
quarkus.datasource.db-kind=postgresql
quarkus.hibernate-orm.schema-management.strategy=drop-and-create
```

## After (Quarkus)

In dev mode the schema is still recreated; in production, Flyway is the one that changes it.

```properties
quarkus.datasource.db-kind=postgresql
%dev.quarkus.hibernate-orm.schema-management.strategy=drop-and-create
quarkus.flyway.migrate-at-start=true
```

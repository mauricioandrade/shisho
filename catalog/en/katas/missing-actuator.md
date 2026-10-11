---
id: missing-actuator
---

## Why

In production, someone needs to know whether the application is alive and ready for traffic:
Kubernetes (liveness and readiness probes), a load balancer, monitoring. The actuator provides
that out of the box at `/actuator/health`, checking the database and other dependencies too, and
publishes metrics through Micrometer.

By default, only `health` is exposed over the web. Endpoints such as `env`, `configprops` and
`heapdump` reveal sensitive information; the Spring Boot documentation recommends exposing only
what is needed and protecting them, with Spring Security or network isolation.

In Quarkus, the actuator's role is split into extensions: `quarkus-smallrye-health` exposes
`/q/health/live` and `/q/health/ready` (the names Kubernetes expects), and `quarkus-micrometer`
takes care of metrics.

## Acceptable when

In applications that do not run as a service (a batch job that ends, a local tool) or when the
platform already monitors them some other way. The starter also widens the exposed surface, so
adding it is a conscious trade-off, not a neutral choice.

## Before

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
```

## After

By default only `/actuator/health` is exposed over the web; expose other endpoints with
`management.endpoints.web.exposure.include`.

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
</dependencies>
```

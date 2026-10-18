---
id: missing-springdoc
---

## Why

An API is a contract with whoever consumes it. Generating the OpenAPI document from the code
keeps the contract in sync with what actually runs: endpoints, parameters, DTOs and validation
annotations show up without hand-written documentation waiting to go stale. Swagger UI lets you
try the API in the browser, and the JSON feeds client generators.

In Quarkus, the `quarkus-smallrye-openapi` extension does this: the document is at `/q/openapi`
and Swagger UI at `/q/swagger-ui`, built from the same Jakarta REST annotations that define the
endpoints.

## Acceptable when

When the API is designed contract-first (API-first): `openapi.yaml` is the source and the code is
generated or validated from it, so generating it back would be redundant. Also in internal APIs,
with a single client from the same team. If the API is not public, consider turning Swagger UI
off in production with `springdoc.swagger-ui.enabled=false`.

An application whose controllers only render views (Thymeleaf, JSP) has no API to document, and
the kata suggests nothing to it.

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

Swagger UI lives at `/swagger-ui.html` and the contract at `/v3/api-docs`. For WebFlux, use
`springdoc-openapi-starter-webflux-ui`.

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>2.8.13</version>
    </dependency>
</dependencies>
```

## Before (Quarkus)

```xml
<dependencies>
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-rest</artifactId>
    </dependency>
</dependencies>
```

## After (Quarkus)

The contract lives at `/q/openapi` and Swagger UI at `/q/swagger-ui`, which by default only
shows up in dev and test; `quarkus.swagger-ui.always-include=true` takes it to production.

```xml
<dependencies>
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-rest</artifactId>
    </dependency>
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-smallrye-openapi</artifactId>
    </dependency>
</dependencies>
```

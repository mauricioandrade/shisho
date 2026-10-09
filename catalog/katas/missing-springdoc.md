---
id: missing-springdoc
---

## Antes

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
```

## Depois

O Swagger UI fica em `/swagger-ui.html` e o contrato em `/v3/api-docs`. Para WebFlux, use
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

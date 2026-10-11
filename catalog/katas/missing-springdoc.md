---
id: missing-springdoc
referencias:
  - https://springdoc.org/
  - https://spec.openapis.org/oas/latest.html
  - https://quarkus.io/guides/openapi-swaggerui
---

## Por quê

Uma API é um contrato com quem a consome. Gerar o documento OpenAPI a partir do código mantém
o contrato sincronizado com o que de fato roda: endpoints, parâmetros, DTOs e anotações de
validação aparecem sem uma documentação escrita à mão para ficar desatualizada. O Swagger UI
deixa testar a API no navegador, e o JSON alimenta geradores de clientes.

No Quarkus, quem faz isso é a extensão `quarkus-smallrye-openapi`: o documento fica em
`/q/openapi` e o Swagger UI em `/q/swagger-ui`, a partir das mesmas anotações Jakarta REST que
definem os endpoints.

## Quando é aceitável

Quando a API é desenhada contrato primeiro (API-first): o `openapi.yaml` é a fonte e o código
é gerado ou validado a partir dele, então gerar de volta seria redundante. Também em APIs
internas, com um único cliente do mesmo time. Se a API não é pública, considere desligar o
Swagger UI em produção com `springdoc.swagger-ui.enabled=false`.

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

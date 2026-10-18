---
id: destructive-schema-generation
referencias:
  - https://docs.spring.io/spring-boot/how-to/data-initialization.html
  - https://quarkus.io/guides/hibernate-orm#dev-mode
  - https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#schema-generation
---

## Por quê

O Hibernate sabe criar as tabelas a partir das entidades, e isso é ótimo para começar: com
`ddl-auto=create-drop` (ou `create`, ou `drop-and-create` no Quarkus), cada partida da aplicação
apaga o schema e o cria de novo, vazio. Em desenvolvimento e nos testes, é exatamente o que se
quer. Em produção, é apagar o banco a cada deploy, a cada restart do container, a cada vez que o
Kubernetes move o pod de máquina.

O perigo é a configuração vazar para onde não devia. No Spring, um `application.properties` vale
para todos os perfis. No Quarkus é mais traiçoeiro: o modo dev já gera o schema sozinho, e uma
chave sem prefixo, que parece "a configuração de desenvolvimento", vale também em produção.

Em produção, o schema é código: muda por migrações versionadas (Flyway, Liquibase), revisadas e
aplicadas uma vez, e o Hibernate só confere (`validate`) ou não mexe (`none`).

## Quando é aceitável

Num banco que nasce e morre com a aplicação: um H2 em memória numa demo, um container de teste. O
Spring Boot já usa `create-drop` sozinho para um banco embutido, sem que ninguém escreva nada. Se
a configuração é só de desenvolvimento, ponha-a num `application-dev.properties` (ou com o prefixo
`%dev.` no Quarkus), e o Shishō deixa de acusar.

## Antes

```properties
spring.datasource.url=jdbc:postgresql://db:5432/loja
spring.jpa.hibernate.ddl-auto=create-drop
```

## Depois

O schema muda por migrações do Flyway (`src/main/resources/db/migration/V1__pedidos.sql`), e o
Hibernate só confere que as entidades batem com ele.

```properties
spring.datasource.url=jdbc:postgresql://db:5432/loja
spring.jpa.hibernate.ddl-auto=validate
```

## Antes (Quarkus)

```properties
quarkus.datasource.db-kind=postgresql
quarkus.hibernate-orm.schema-management.strategy=drop-and-create
```

## Depois (Quarkus)

No modo dev o schema continua sendo recriado; em produção, quem muda o schema é o Flyway.

```properties
quarkus.datasource.db-kind=postgresql
%dev.quarkus.hibernate-orm.schema-management.strategy=drop-and-create
quarkus.flyway.migrate-at-start=true
```

---
id: sql-logging
referencias:
  - https://docs.spring.io/spring-boot/appendix/application-properties/index.html#application-properties.data.spring.jpa.show-sql
  - https://quarkus.io/guides/hibernate-orm#quarkus-hibernate-orm_quarkus-hibernate-orm-log-sql
  - https://vladmihalcea.com/log-sql-spring-boot/
---

## Por quê

Ver o SQL que o Hibernate gera é uma das melhores formas de aprender JPA e de achar um N+1. Por
isso o `spring.jpa.show-sql=true` aparece em tanto tutorial. O problema é ele ir junto para
produção.

Em produção, cada requisição vira várias linhas de log, e o log é pago: em disco, no agregador
(Datadog, CloudWatch, Loki) e no tempo de quem procura um erro no meio do ruído. O `show-sql`
escreve no `System.out`, por fora do sistema de log, sem nível nem formato. E, com o logger dos
parâmetros ligado, os valores vão junto: e-mails, CPFs, o que estiver na consulta.

## Quando é aceitável

Num perfil de desenvolvimento (`application-dev.properties`, `%dev.` no Quarkus), que o Shishō não
acusa. Para investigar uma consulta em produção, prefira ligar o logger `org.hibernate.SQL` por um
tempo (o actuator do Spring muda o nível em tempo de execução, em `/actuator/loggers`) ou olhar as
métricas e o log de consultas lentas do Hibernate.

## Antes

```properties
spring.jpa.show-sql=true
```

## Depois

O SQL continua visível em desenvolvimento: a mesma chave, em `application-dev.properties`.

```properties
spring.jpa.show-sql=false
```

## Antes (Quarkus)

```properties
quarkus.hibernate-orm.log.sql=true
```

## Depois (Quarkus)

```properties
%dev.quarkus.hibernate-orm.log.sql=true
```

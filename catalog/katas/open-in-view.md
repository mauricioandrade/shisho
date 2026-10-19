---
id: open-in-view
referencias:
  - https://docs.spring.io/spring-boot/reference/data/sql.html#data.sql.jpa-and-spring-data.open-entity-manager-in-view
  - https://github.com/spring-projects/spring-boot/issues/7107
  - https://vladmihalcea.com/the-open-session-in-view-anti-pattern/
---

## Por quê

Com Spring Data JPA, o Spring Boot liga sozinho o **Open Session in View** (OSIV): o
`EntityManager` abre no começo da requisição e só fecha quando a resposta sai. A intenção é boa.
Um relacionamento `LAZY` lido no controller, ou pelo Jackson ao montar o JSON, funciona em vez de
dar `LazyInitializationException`.

O preço aparece com carga. Cada requisição segura uma conexão do pool durante todo o tratamento,
inclusive enquanto chama outros serviços ou escreve a resposta, e o pool esgota bem antes do que
devia. As consultas que o OSIV permite acontecem fora do service, sem transação, uma por
relacionamento lido (o N+1), e escondidas na serialização, onde ninguém procura.

A discussão é antiga o bastante para o próprio Spring Boot avisar no log, a cada partida, quando
ninguém declarou a propriedade: `spring.jpa.open-in-view is enabled by default`.

## Quando é aceitável

Quando o projeto escolheu o OSIV, sabendo o custo: uma aplicação com páginas Thymeleaf e pouco
tráfego, por exemplo. Basta declarar `spring.jpa.open-in-view=true` e o Shishō aceita, porque a
escolha ficou escrita. A kata também não acusa um módulo que importa a configuração de um config
server, porque a propriedade pode estar lá.

## Antes

```properties
spring.datasource.url=jdbc:postgresql://db:5432/loja
```

## Depois

Com o OSIV desligado, o service carrega o que a resposta precisa (`JOIN FETCH`, `@EntityGraph`
ou uma projeção em DTO), e um acesso lazy esquecido falha no teste em vez de virar consulta escondida
em produção.

```properties
spring.datasource.url=jdbc:postgresql://db:5432/loja
spring.jpa.open-in-view=false
```

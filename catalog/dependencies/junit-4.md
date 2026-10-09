---
id: junit-4
nome: JUnit 4
coordenadas:
  - junit:junit
resumo: em modo de manutenção, só com correções críticas, e fora do starter de testes do Spring Boot desde a 2.4
alternativa: JUnit Jupiter (JUnit 5+), que já vem no spring-boot-starter-test
referencias:
  - https://junit.org/junit4/
  - https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-2.4-Release-Notes
---

## Por quê

O site do JUnit 4 diz que ele está em modo de manutenção: só bugs críticos e falhas de segurança
são corrigidos, e o desenvolvimento seguiu para o JUnit Jupiter. Desde o Spring Boot 2.4, o
`spring-boot-starter-test` não traz mais o motor que roda testes do JUnit 4 (o vintage).

Manter os dois no mesmo projeto significa duas formas de escrever teste (`@Before` e
`@BeforeEach`, `@Rule` e extensões) e o vintage declarado à mão para rodar os antigos. A
migração costuma ser mecânica, e o vintage permite fazê-la aos poucos.

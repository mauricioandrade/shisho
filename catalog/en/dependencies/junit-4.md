---
id: junit-4
nome: JUnit 4
resumo: in maintenance mode, with critical fixes only, and out of Spring Boot's test starter since 2.4
alternativa: JUnit Jupiter (JUnit 5+), which spring-boot-starter-test already ships
---

## Why

The JUnit 4 site says it is in maintenance mode: only critical bugs and security issues are
fixed, and development moved on to JUnit Jupiter. Since Spring Boot 2.4,
`spring-boot-starter-test` no longer ships the engine that runs JUnit 4 tests (vintage).

Keeping both in one project means two ways of writing tests (`@Before` and `@BeforeEach`,
`@Rule` and extensions) and the vintage engine declared by hand to run the old ones. The
migration is usually mechanical, and vintage lets you do it gradually.

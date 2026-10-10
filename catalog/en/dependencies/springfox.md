---
id: springfox
nome: Springfox
resumo: no new release since July 2020, and no support for Spring Boot 3
alternativa: springdoc-openapi (org.springdoc:springdoc-openapi-starter-webmvc-ui)
---

## Why

Springfox's last release, 3.0.0, came out in July 2020, before Spring Boot 3 moved to Jakarta
EE. The question "any version compatible with Spring Boot 3?" went unanswered in its repository
until it was closed automatically for inactivity. In practice it becomes a lock: as long as the
project depends on it, Spring cannot be upgraded.

springdoc-openapi does the same job, generates OpenAPI 3 and keeps up with Spring Boot's
versions. Its documentation has a migration guide, with the replacement for each Swagger 2
annotation (`@Api` → `@Tag`, `@ApiOperation` → `@Operation`, `@ApiModel` → `@Schema`).

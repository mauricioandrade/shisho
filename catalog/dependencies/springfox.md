---
id: springfox
nome: Springfox
coordenadas:
  - io.springfox:springfox-boot-starter
  - io.springfox:springfox-swagger2
  - io.springfox:springfox-swagger-ui
resumo: sem versão nova desde julho de 2020 e sem suporte ao Spring Boot 3
alternativa: springdoc-openapi (org.springdoc:springdoc-openapi-starter-webmvc-ui)
referencias:
  - https://github.com/springfox/springfox/releases
  - https://github.com/springfox/springfox/issues/4075
  - https://springdoc.org/#migrating-from-springfox
---

## Por quê

A última versão do Springfox, a 3.0.0, saiu em julho de 2020, antes da mudança do Spring Boot 3
para o Jakarta EE. A pergunta "alguma versão compatível com o Spring Boot 3?" ficou sem resposta
no repositório até ser fechada automaticamente por inatividade. Na prática, ele vira uma trava:
enquanto o projeto depender dele, o Spring não é atualizado.

O springdoc-openapi cumpre o mesmo papel, gera OpenAPI 3 e acompanha as versões do Spring Boot.
A documentação dele tem um guia de migração, com a troca de cada anotação do Swagger 2
(`@Api` → `@Tag`, `@ApiOperation` → `@Operation`, `@ApiModel` → `@Schema`).

# Changelog

As mudanças de cada versão do Shishō. A seção de uma versão vira as notas da release no GitHub,
em português e em inglês ([CHANGELOG.en.md](CHANGELOG.en.md)). O formato segue o
[Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/), e as versões, o
[SemVer](https://semver.org/lang/pt-BR/): enquanto for 0.x, uma versão menor nova pode mudar o
comportamento.

## [Não lançado]

### Novo

- **Katas de JPA**: `entity-without-default-constructor` (GRAVE), `eager-collection`,
  `to-one-without-lazy` e `transactional-controller` ([ADR 0021](docs/adr/0021-jpa-katas.md)).

## [0.2.0] - 2026-10-18

### Atenção

- A kata nova `unproxyable-method` é **GRAVE**: um `@Transactional` num método `private` passa a
  dar código de saída 1, e um CI que roda o Shishō pode falhar onde antes passava.
- Projetos em Java 8 a 20 deixam de ser recusados (antes, o código de saída era 3).

### Novo

- **Quarkus**: as katas, o modo shoshin e a classificação reconhecem CDI, Jakarta REST e Panache;
  kata `quarkus-private-injection` ([ADR 0012](docs/adr/0012-quarkus.md)).
- **Gradle**: `build.gradle` e `build.gradle.kts` lidos sem executar o script, com os version
  catalogs (bibliotecas, bundles e catálogos nomeados no `settings.gradle`)
  ([ADR 0013](docs/adr/0013-gradle.md)).
- **`shisho crud` no Quarkus**, nas três arquiteturas ([ADR 0014](docs/adr/0014-quarkus-crud.md)).
- **Multi-módulo**: os módulos do Maven (de profiles também) e do Gradle (com `projectDir`), e os
  convention plugins do `buildSrc`; cada lição vai para o build que a declara, e o hook do Claude
  Code sobe até a raiz ([ADR 0015](docs/adr/0015-multi-module.md)).
- **Java 8 a 20 e Spring Boot 2**: o que precisa de uma versão mais nova vira sugestão "Ao migrar
  para o Java X"; o Spring Boot 2 é reconhecido pela versão que o build fixa ou pelos imports
  `javax.*`, e o `shisho fix` importa `javax.validation.Valid` ali
  ([ADR 0017](docs/adr/0017-java-17.md), [ADR 0018](docs/adr/0018-java-8-and-javax.md)).
- **Katas novas**: `unproxyable-method`, `swallowed-exception` e `entity-with-lombok-data`
  ([ADR 0019](docs/adr/0019-new-katas.md)). A `unproxyable-method` também acusa a chamada de
  dentro da própria classe (`this.salvar()`), que escapa do proxy
  ([ADR 0020](docs/adr/0020-self-invocation.md)).

### Corrigido

Falsos positivos achados ao rodar o Shishō no spring-petclinic, no JHipster, no buckpal, no
eladmin e em outros projetos reais ([ADR 0016](docs/adr/0016-real-projects.md)):

- `request-body-without-valid` não cobra `@Valid` de um DTO sem nenhuma restrição, nem de um corpo
  só com tipos do JDK (`Set<Long>`, `List<String>`) ou de um `MultipartFile`;
- `lombok-constructor` olha só beans, inclusive os de estereótipos próprios (`@UseCase`);
- `missing-controller-advice` e `missing-springdoc` ignoram controllers que só renderizam views;
- `dto-not-record` ignora builders e classes sem estado;
- testes em outros source sets (`src/integration-test`, `src/testFixtures`…) não são analisados.

### Mudou

- O `shisho crud` recusa projetos abaixo do Java 17 ou ainda em `javax.*` / Spring Boot 2: ele gera
  código Spring Boot 3 e Quarkus 3.

## [0.1.1] - 2026-10-10

### Mudou

- O repositório passou a se chamar `shisho`.

## [0.1.0] - 2026-10-10

A primeira versão: katas de Spring Boot com graduação, classificação de arquitetura e veredito de
overengineering, relatórios no terminal, em HTML e em SARIF, GitHub Action, modo sensei com
`shisho fix`, `shisho ask` com LLM local, `shisho crud`, português e inglês, plugin para o Claude
Code e modo shoshin.

[Não lançado]: https://github.com/mauricioandrade/shisho/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/mauricioandrade/shisho/compare/v0.1.1...v0.2.0
[0.1.1]: https://github.com/mauricioandrade/shisho/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/mauricioandrade/shisho/releases/tag/v0.1.0

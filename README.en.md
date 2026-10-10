<div align="center">

# 師匠 Shishō

**The master who reads your code, points out what is wrong and explains why.**

Starting with Java and Spring Boot.

[Português](README.md) · **English**

![Java 25](https://img.shields.io/badge/runs%20on-Java%2025-orange)
![Analyzes Java 21+](https://img.shields.io/badge/analyzes-Java%2021%2B-blue)
![License](https://img.shields.io/badge/license-Apache%202.0-green)

![Gojo Satoru, from Jujutsu Kaisen, pulling down his blindfold](docs/anime/hero.gif)

*The Six Eyes see everything. The Shishō sees your field `@Autowired`.*

</div>

---

> **Language note.** The Shishō speaks Brazilian Portuguese today: the reports, the catalog, the
> generated code and the ADRs in `docs/adr` are in pt-BR. The terminal samples below are real
> output, shown as is. The commands, options and exit codes work the same in any language.

## What it is

Linters tell you **what** is wrong. The Shishō is opinionated and didactic: it tells you what is
wrong, **why** it matters and which way to go, the way a sensei would teach a student.

![Ichigo, from Bleach, drinking from a juice box, utterly bored](docs/anime/linters.gif)

*You, reading 300 linter warnings with no explanation at all.*

- 🥋 **Katas**: Java/Spring Boot good-practice rules. Every violation becomes a **lesson** with an
  explanation.
- 🏯 **Architecture classification**: recognizes whether the project is layered,
  package-by-feature, modular monolith, hexagonal, clean, CQRS, event-driven or microservices.
- ⚖️ **Overengineering verdict**: weighs what is in the code against the project's context and
  says whether the architecture is too big (or too small) for the problem.
- 🎖️ **Rank**: from 6th kyū to 1st dan.
- 🧠 **Optional local LLM**: a model running on your machine explains the verdict. It explains;
  it never decides.
- 🏗️ **CRUD generator**: creates a REST CRUD in the project's architecture and explains the role
  of every file. The generated code passes every kata.

## Demo

```
$ shisho analyze ./clareza-api

師匠 Shishō — análise de clareza-api  (Java 21)
Arquitetura: Camadas técnicas (Layered)  (83% da assinatura: controller, service, repository, model, dto)

✗ GRAVE  [field-injection]             UsuarioController.java:14
         usuarioService é injetado direto no campo. Assim, new cria a classe sem a dependência, o
         erro só aparece como NullPointerException em uso e o teste precisa de Spring ou reflexão.
         Receba a dependência pelo construtor, num campo private final.
! MEDIA  [request-body-without-valid]  PedidoController.java:27
         @RequestBody CriarPedidoRequest sem @Valid: as anotações de validação do DTO são ignoradas,
         e dados inválidos chegam ao service como se fossem válidos. Adicione @Valid antes do
         @RequestBody.
! MEDIA  [entity-in-controller]        PedidoController.java:31
         O endpoint buscar() expõe a entidade JPA Pedido na API. Use records de request/response: o
         esquema do banco deixa de ser o contrato da API, campos internos não vazam e o cliente não
         consegue sobrescrevê-los.

Veredito: subengenharia. O projeto já não cabe na arquitetura Camadas técnicas (Layered):
  · PedidoService tem 31 métodos e 9 dependências no construtor.
Próximo passo natural: package-by-feature.

Graduação: 3º kyū  (8 lições: 1 grave, 4 médias, 3 leves)
```

Each lesson says what is wrong, what goes wrong because of it and how to fix it. In the sample:
a field-injected dependency, a request body whose validation never runs and a JPA entity
exposed by the API. The verdict says the layered architecture no longer fits: one service has 31
methods and 9 dependencies, and the next step is package-by-feature.

## The rank

Every lesson takes points off. The final score becomes a rank:

| Score | Rank |
|---|---|
| 92–100 | 1st dan |
| 85–91 | 1st kyū |
| 75–84 | 2nd kyū |
| 65–74 | 3rd kyū |
| 55–64 | 4th kyū |
| 40–54 | 5th kyū |
| 0–39 | 6th kyū |

![Jiraiya, from Naruto, snapping a double popsicle in half and offering one half to Naruto](docs/anime/graduation.gif)

*No severe lessons. The master shares the popsicle.*

## Katas

| Kata | Severity | What it flags | Why it matters |
|---|---|---|---|
| `field-injection` | GRAVE | `@Autowired`/`@Inject` on a field | `new` creates an incomplete object, and the test needs Spring |
| `lombok-constructor` | MEDIA | `@RequiredArgsConstructor`/`@AllArgsConstructor` | the dependencies vanish from the code, and the tenth one slips in unnoticed |
| `non-final-dependency` | MEDIA | injected dependency without `final` | any method can replace it after construction |
| `system-out` | MEDIA | `System.out`, `System.err`, `printStackTrace()` | no level or timestamp, and outside the log aggregator |
| `request-body-without-valid` | MEDIA | `@RequestBody` without `@Valid` | the DTO's validation is ignored, and invalid data reaches the service |
| `entity-in-controller` | MEDIA | endpoint that receives or returns a JPA `@Entity` | the database becomes the API contract, and internal fields leak |
| `missing-controller-advice` | MEDIA | controllers and no `@RestControllerAdvice` at all | every endpoint handles (or forgets) its own errors |
| `avoided-dependency` | MEDIA | abandoned, replaced or relocated dependency (Log4j 1, Springfox, Joda-Time... see [`catalog/dependencies`](catalog/dependencies)) | no security fixes, or already replaced by its own project |
| `value-outside-constructor` | LEVE | `@Value` outside a constructor parameter | the value arrives by reflection, and a test using `new` goes without it |
| `dto-not-record` | LEVE | DTO (by package or suffix) that is not a record | hand-written getters, `equals` and `hashCode`, and mutable |
| `transactional-read-only` | LEVE | read method (`find…`, `buscar…`) in a write transaction | Hibernate dirty-checks for nothing |
| `configuration-properties-not-record` | LEVE | `@ConfigurationProperties` on a class with setters | the configuration can change after it is loaded |
| `prefer-configuration-properties` | LEVE | 3 or more `@Value` with the same prefix in one class | loose properties, with no type or validation in one place |
| `missing-actuator` | LEVE | web application without `spring-boot-starter-actuator` | with no health check, the orchestrator cannot tell whether the app is alive |
| `missing-springdoc` | INFO | web application without springdoc-openapi | API consumers get no contract and no Swagger UI |
| `prefer-virtual-threads` | INFO | `Executors.newFixedThreadPool`/`newCachedThreadPool` (Java 21) | for I/O, virtual threads need no pool sizing |
| `prefer-scoped-values` | INFO | `ThreadLocal` field (Java 25) | `ScopedValue` does not leak between tasks and needs no `remove()` |

The severities are GRAVE (severe), MEDIA (medium), LEVE (minor) and INFO. Suggestions (INFO)
cost no points. With `--sensei`, every lesson comes with the full explanation, the cases where
the flagged code is acceptable and a before/after example.

Every project has its reasons. A `shisho.yaml` at the root of the analyzed project turns katas
off or changes their severity:

```yaml
# shisho.yaml
katas:
  system-out:
    ativa: false          # turns the kata off
  dto-not-record:
    severidade: info      # grave | media | leve | info
```

Violations of the detected architecture's dependency rules show up as the `dependency-rule`
kata (MEDIA) and are configured the same way.

An unknown kata or a misspelled field makes the analysis fail (exit code 3). That beats a
configuration that silently does nothing.

## Overengineering

The question the Shishō likes answering most: **does this architecture fit this project?**

Overengineering is not a property of the code alone. It is the relation between the complexity
of the solution and that of the problem. So the Shishō puts two things together: the **facts
measured** in the code (one use case per CRUD operation, a JPA entity identical to the domain
object, mappers between identical structures, a service that does everything…) and the
**context** you declare:

```yaml
# shisho.yaml
contexto:
  tipo: mvp              # mvp | portfolio | produto | corporativo
  time: 1                # people on the project (10 or more counts as a large team)
  vida_util: curta       # expected lifetime: curta | media | longa (short | medium | long)
  caracteristicas:       # anything else that justifies a bigger architecture, as catalog keys
    - dominio_com_regras_complexas   # a domain with complex rules
```

First the Shishō recognizes the architecture from the package structure and checks its
dependency rules (a `domain` package importing Spring becomes a lesson). Then comes the verdict:

```
Arquitetura: Clean Architecture  (100% da assinatura: domain, application, infrastructure, usecase, domain/model, domain/port)

Veredito: overengineering. A arquitetura Clean Architecture custa de 7 a 12 classes por
funcionalidade, e nada no contexto declarado paga esse custo. Ela vale a pena quando:
dominio_com_regras_complexas, vida_util_longa, troca_provavel_de_infra, time_grande.
  · Um mapper converte UsuarioJpaEntity em Usuario, que têm exatamente os mesmos campos.
  · Usuario e a entidade JPA UsuarioJpaEntity têm exatamente os mesmos campos: duas cópias da mesma
    estrutura, mais o mapeamento entre elas.
  · 4 use cases só para operações CRUD de Usuario: um use case por método, sem regra de negócio que
    justifique cada um.
Considere package-by-feature.
```

In short: Clean Architecture costs 7 to 12 classes per feature, nothing in the declared context
pays for it, and the code shows the classic signs (a mapper between identical structures, a JPA
entity mirroring the domain, one use case per CRUD method). Consider package-by-feature.

It works the other way too: a layered project with a 30-method service, or services that depend
on each other, gets an **underengineering** verdict and the next step up.

![Dazai, from Bungou Stray Dogs, stuck upside down in a drum, suffering](docs/anime/overengineering.gif)

*Twelve classes for a CRUD. Getting in was easy.*

What the Shishō knows about each architecture lives in
[`catalog/architectures`](catalog/architectures), in Markdown: when it is worth the cost and
when it is overkill.

## The LLM explains, it does not judge

Everything the Shishō decides is deterministic: same input, same result, ready for CI. The local
LLM (through [Ollama](https://ollama.com)) is **optional and off by default**: only `shisho ask`
talks to it. The model receives the facts and the verdict already computed, plus the catalog's
text about the detected and the suggested architecture, and only writes the explanation:

```
$ shisho ask ./loja-api "por que não Clean Architecture aqui?"
Perguntando ao gemma3:4b em http://localhost:11434...
Aluno, a análise indica que o projeto apresenta um overengineering significativo, utilizando a
Clean Architecture de forma excessiva. [...]

Observe que o mapper converte `UsuarioJpaEntity` em `Usuario` com exatamente os mesmos campos.
[...] Os quatro use cases para operações CRUD de `Usuario` também são um exemplo de excesso, com
um use case por método, sem necessidade de regras de negócio que justifiquem essa separação.
[...]

Explicação escrita por gemma3:4b, que pode errar. O veredito, a graduação e as lições vêm da
análise determinística: shisho analyze.
```

Without a question, it explains the verdict, the rank and where to start. To use it:

```bash
# install Ollama: https://ollama.com/download
ollama pull gemma3:4b                                  # the default model, ~3 GB
java -jar target/shisho.jar ask ~/projects/my-api      # explains the verdict
java -jar target/shisho.jar ask --model qwen2.5:7b ~/projects/my-api "which lesson should I fix first?"
```

`gemma3:4b` is light, but it sometimes swaps a severity or a detail; bigger models (7B to 14B)
tend to stick to the facts better. `--url` points to an Ollama on another machine. Why Ollama,
why no SDK, and how the prompt is built: [ADR 0004](docs/adr/0004-local-llm.md).

![Gintoki, from Gintama, in a lab coat, writing his own name on the board while the class shouts "Sensei!"](docs/anime/sensei.gif)

*Ginpachi-sensei teaches the class. The grade comes from Java.*

## Java versions

The Shishō runs on **Java 25** and analyzes projects from **Java 21** on. It reads your project's
version and **only asks for what that version allows**. Anything that only exists in a newer
version becomes a suggestion that costs no points:

```
i INFO   [prefer-scoped-values]  NotificacaoService.java:11
         Ao migrar para o Java 25: O campo USUARIO_ATUAL é um ThreadLocal. ...
```

Preview features are never recommended. What the Shishō knows about each version lives in
[`catalog/java-features`](catalog/java-features).

![Super Saiyan Goku, from Dragon Ball Z, turning to the camera with a smile and a thumbs up](docs/anime/java-versions.gif)

*Moved your project to Java 25? The Shishō unlocks the new katas.*

## Roadmap

- [x] **Phase 1**: CLI, catalog, 7 AST katas, terminal report
- [x] **Phase 2**: project katas (JPA, transactions, error handling, build, configuration) and
  `shisho.yaml`
- [x] **Phase 3**: architecture classification, dependency rules and the
  overengineering/underengineering verdict
- [x] **Phase 4**: HTML report, SARIF and GitHub Action
- [x] **Phase 5**: sensei mode (before/after examples), Java features catalog, `shisho fix`
- [x] **Phase 6**: optional local LLM (`shisho ask`)
- [x] **Phase 7**: CRUD generator (`shisho crud`): layered, package-by-feature and hexagonal,
  with an overengineering warning before generating
- [ ] **Next**: a CRUD template for clean; other stacks, reusing the architecture catalog
- [ ] **Ideas**: a `--shoshin` mode (初心, "beginner's mind") that explains every concept found
  in the code (what a record is, a sealed interface…) and design patterns

## Running from source

You need **JDK 25** and **Maven 3.9**. If you already have both, skip straight to `mvn verify`.

If you don't (or you have another Java installed for other projects), the repository ships a
[`mise.toml`](mise.toml) with the exact versions. [mise](https://mise.jdx.dev) is a tool version
manager, similar to SDKMAN, asdf or nvm, but for several languages at once. It reads `mise.toml`
and, **only inside this folder**, puts the right Java and Maven on the `PATH`, without touching
what you use on the rest of your machine.

```bash
# install mise: https://mise.jdx.dev/getting-started.html
mise trust      # trusts this repository's mise.toml (first time only)
mise install    # downloads the pinned Temurin 25 and Maven
mvn verify      # tests + target/shisho.jar
```

Then point the Shishō at the root of a Maven project:

```bash
java -jar target/shisho.jar analyze ~/projects/my-api
```

Besides the terminal, the report comes out as SARIF (for GitHub code scanning and other tools)
or HTML (a single page, no dependencies, light and dark themes):

```bash
java -jar target/shisho.jar analyze --format sarif --output shisho.sarif ~/projects/my-api
java -jar target/shisho.jar analyze --format html  --output shisho.html  ~/projects/my-api
```

The exit code is designed for CI and does not depend on the format:

| Code | Meaning |
|---|---|
| 0 | no severe lesson |
| 1 | at least one **GRAVE** (severe) lesson |
| 2 | invalid input (missing path or argument, an `--output` that cannot be written; in `crud`, an architecture nothing pays for) |
| 3 | the project could not be analyzed (e.g. Java below 21, invalid `shisho.yaml`); in `fix --apply`, a file that could not be written; in `ask`, Ollama down or the model not installed |

mise is only a development convenience. The Shishō does not depend on it, and nothing in the
build requires it.

## Sensei mode: learn and fix

With `--sensei`, the report explains every kata it found: **why** it exists, **when** the flagged
code is a reasonable choice, the code **before and after** the fix and the **sources** (Spring
docs, JEPs, OWASP). A kata points at a cost, not a sin: the sensei shows the trade-off so you can
decide. The guides live in [`catalog/katas`](catalog/katas), and a test makes sure the "before"
triggers the lesson and the "after" does not. The HTML report and the alert help on GitHub show
the same content.

```bash
java -jar target/shisho.jar analyze --sensei ~/projects/my-api
```

Some lessons have a safe mechanical fix, and the Shishō applies it for you:

```bash
java -jar target/shisho.jar fix ~/projects/my-api            # shows the diff
java -jar target/shisho.jar fix --apply ~/projects/my-api    # writes it
```

![Joseph Joestar, from JoJo's Bizarre Adventure, holding Hermit Purple with the look of someone who already knows what comes next](docs/anime/autofix.gif)

*"Your next line will be: `private final UsuarioService service;`" `fix` shows the diff before writing.*

| Kata | What `fix` does |
|---|---|
| `field-injection` | removes `@Autowired`, makes the field `final` and receives it through the constructor |
| `lombok-constructor` | replaces the annotation with the constructor Lombok would generate |
| `non-final-dependency` | adds `final`, if every constructor assigns the field and nothing else changes it |
| `request-body-without-valid` | adds `@Valid`, if the build has Bean Validation |
| `transactional-read-only` | sets `readOnly = true` |

Only the fixed lines change: formatting and comments stay as they were. When the fix is not
safe (a `@Qualifier` that would have to move, a `@Builder` that relies on Lombok's
constructor…), the lesson is left to you, with the sensei mode example. Why JavaParser and not
OpenRewrite: [ADR 0003](docs/adr/0003-autofix.md).

## From scratch: `shisho crud`

The Spring Initializr sets up the build and the dependencies, but it does not tell you where each
class goes or why. `shisho crud` creates a complete REST CRUD (entity, repository, DTOs, service,
controller and error handling) in the project's architecture, and explains the role of every
file:

```
$ shisho crud --arquitetura package-by-feature ./loja Produto nome:String preco:BigDecimal
師匠 Shishō — CRUD de Produto em Pacotes por funcionalidade (Package by Feature)

+ src/main/java/com/exemplo/loja/produto/Produto.java
  A entidade JPA: a tabela do banco vista como objeto. Como todas as classes de `produto`, ela é
  package-private (sem `public`): nenhuma outra feature enxerga a entidade, e as features
  conversam pelo service. [...]

+ src/main/java/com/exemplo/loja/produto/ProdutoService.java
  Onde mora a regra de negócio e onde começa a transação. A classe inteira é `readOnly = true`, e
  só os métodos que escrevem abrem uma transação de escrita. [...]
...
Criados 8 arquivos. Rode shisho analyze para conferir: o código gerado passa em todas as katas.
```

![Sasuke, from Naruto, with his Sharingan active and a half smile](docs/anime/crud.gif)

*The Sharingan copies the jutsu and understands every hand sign. `crud` copies the project's architecture and explains every file.*

- **Architecture:** `layered`, `package-by-feature` or `hexagonal`. Without `--arquitetura`, it
  follows the one the Shishō detects in the project. A project fresh from the Initializr has
  nothing to detect yet, so the first `crud` needs the option. If you ask for an architecture
  other than the project's, it generates and warns that the styles will mix.
- **Fields:** `name:Type`, with `String`, `Integer`, `Long`, `Double`, `Boolean`, `BigDecimal`,
  `LocalDate`, `LocalDateTime`, `Instant` or `UUID`. Text is validated with `@NotBlank`, the rest
  with `@NotNull`.
- **Never overwrites.** If any file already exists, nothing is written. Files shared by every
  entity, such as the `@RestControllerAdvice`, are only created if the project has none yet.
- **Standard errors.** 404s and validation errors answer with ProblemDetail (RFC 9457), naming
  the fields that failed.

![Joseph Joestar, from JoJo's Bizarre Adventure, running while someone shouts "Run away!"](docs/anime/never-overwrites.gif)

*The file already exists? Nigerundayo! The Shishō backs off without writing anything.*

### Before generating: why?

Hexagonal costs more files than a CRUD usually needs. Before generating it, the Shishō asks for
the reason, and without one it writes nothing:

```
$ shisho crud --arquitetura hexagonal ./loja Produto nome:String preco:BigDecimal
⚠ Este CRUD em Hexagonal (Ports & Adapters) cria 12 arquivos, e nada no contexto do shisho.yaml paga
esse custo. A arquitetura vale a pena quando o projeto tem: multiplas_integracoes_externas,
dominio_com_regras_complexas, troca_provavel_de_infra, vida_util_longa. Para um CRUD sem regras de
negócio, package-by-feature faz o mesmo com menos arquivos.

Nada foi gravado. Se o projeto tem um desses motivos, declare-o em contexto.caracteristicas no
shisho.yaml: a decisão fica registrada, e o shisho analyze passa a usá-la. Para gerar mesmo assim,
use --mesmo-assim.
```

In short: this CRUD would create 12 files, and nothing in the `shisho.yaml` context pays for
that. Hexagonal is worth it with several external integrations, a domain with complex rules,
infrastructure likely to change or a long lifetime. For a CRUD with no business rules,
package-by-feature does the same with fewer files.

![Gintoki, from Gintama, holding a carton of strawberry milk, in disbelief](docs/anime/cost-warning.gif)

*Hexagonal. For a two-field CRUD. Seriously?*

With the reason declared in [`shisho.yaml`](#overengineering), the CRUD is generated right away,
and the report names the reason that paid for it. `--mesmo-assim` ("anyway") is for when you just
want to see what the architecture looks like. In a project that is already hexagonal, the next
`crud` goes ahead without asking: the decision was already made. Why it works this way:
[ADR 0006](docs/adr/0006-crud-cost-warning.md).

![Dazai, from Bungou Stray Dogs, kneeling in a garden and laughing](docs/anime/cost-paid.gif)

*With the reason declared in `shisho.yaml`, Dazai gets out of the drum.*

A test applies every template to a project like the Initializr's and requires `shisho analyze`
to recognize the architecture and find **no** lesson at all. The code was also checked on a real
Initializr project (Spring Boot 4.1): it compiles, starts and answers. The templates live in
[`catalog/scaffolds`](catalog/scaffolds), in Markdown. Why it works this way:
[ADR 0005](docs/adr/0005-crud-scaffolding.md).

## On GitHub Actions

```yaml
jobs:
  shisho:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      security-events: write   # to upload the SARIF to code scanning
    steps:
      - uses: actions/checkout@v4
      - uses: mauricioandrade/shiso@main
        with:
          path: .                # project to analyze (default: repository root)
          fail-on-grave: true    # fails the job on a GRAVE lesson (default: true)
          upload-sarif: true     # lessons as code scanning alerts (default: true)
          html: shisho.html      # optional: also writes the HTML report
```

Lessons show up as code scanning alerts and pull request annotations; the terminal report goes
to the job summary. The action installs Java 25 and builds the Shishō from its own source. Since
that changes the job's Java, **run it in a job separate** from your project's build.

## Contributing

The easiest way to contribute requires no Java at all: **the catalog is Markdown**. A new
architecture, a missing overengineering signal, a clearer explanation, a CRUD template for
another architecture: each one is a PR in `catalog/`. The schema is in
[`catalog/README.md`](catalog/README.md).

![Naruto making the Shadow Clone Jutsu hand sign](docs/anime/contributing.gif)

*One dev alone can't cover every kata. We need clones.*

Issues labeled `good first issue` are a good starting point.

## License

Code under [Apache 2.0](LICENSE). The anime GIFs belong to their creators and studios and are
used for illustration only.

## Credits

- [mise](https://github.com/jdx/mise), by Jeff Dickey and contributors (MIT): Java and Maven
  version management during development.
- [JavaParser](https://github.com/javaparser/javaparser) (Apache 2.0 / LGPL): the Java parser
  behind the katas. The choice is in [`docs/adr/0001-parser.md`](docs/adr/0001-parser.md).
- [picocli](https://picocli.info) (Apache 2.0): command line.
- [SnakeYAML](https://bitbucket.org/snakeyaml/snakeyaml) (Apache 2.0): reads the catalog's
  frontmatter and Ollama's responses.
- [Ollama](https://ollama.com) (MIT): server for the optional local LLM; not bundled in the jar.

<div align="center">

# 師匠 Shishō

**The master who reads your code, points out what is wrong and explains why.**

Starting with Java, with Spring Boot and Quarkus.

[![CI](https://github.com/mauricioandrade/shisho/actions/workflows/ci.yml/badge.svg)](https://github.com/mauricioandrade/shisho/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/mauricioandrade/shisho?label=release)](https://github.com/mauricioandrade/shisho/releases)
![Runs on Java 25](https://img.shields.io/badge/runs%20on-Java%2025-orange?logo=openjdk)
![Analyzes Java 21+](https://img.shields.io/badge/analyzes-Java%2021%2B-blue?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3%20%7C%204-6DB33F?logo=springboot&logoColor=white)
![Quarkus](https://img.shields.io/badge/Quarkus-3-4695EB?logo=quarkus&logoColor=white)
[![License Apache 2.0](https://img.shields.io/badge/license-Apache%202.0-green)](LICENSE)
![Languages: pt-BR and en](https://img.shields.io/badge/languages-pt--BR%20%7C%20en-informational)
[![Claude Code plugin](https://img.shields.io/badge/Claude%20Code-plugin-D97757)](#-in-claude-code)

[Português](README.md) · **English**

![Gojo Satoru, from Jujutsu Kaisen, pulling down his blindfold](docs/anime/hero.gif)

*The Six Eyes see everything. The Shishō sees your field `@Autowired`.*

</div>

---

**Getting started:** [What it is](#-what-it-is) · [Why it exists](#-why-it-exists) · [Installation](#-installation) · [Quick start](#-quick-start) · [Demo](#-demo)

**Guide:** [Katas](#-katas) · [The rank](#-the-rank) · [Sensei mode](#-sensei-mode-learn-and-fix) · [Shoshin mode](#-shoshin-mode-the-concepts-in-your-code) · [Overengineering](#-overengineering) · [`shisho crud`](#-from-scratch-shisho-crud) · [Local LLM](#-the-llm-explains-it-does-not-judge) · [In Claude Code](#-in-claude-code) · [Java versions](#-java-versions) · [Reports](#-reports-and-exit-codes) · [On GitHub Actions](#-on-github-actions)

**Project:** [Development](#-development) · [Roadmap](#-roadmap) · [Contributing](#-contributing) · [License](#-license) · [Credits](#-credits)

---

> **Language.** The Shishō speaks Brazilian Portuguese by default and English with
> `SHISHO_IDIOMA=en`, which the installer sets for you. Reports, sensei guides and the code
> `shisho crud` generates all follow it. The ADRs in `docs/adr` are in Portuguese.

## 🏯 What it is

Linters tell you **what** is wrong. The Shishō is opinionated and didactic: it tells you what is
wrong, **why** it matters and which way to go, the way a sensei would teach a student.

![Ichigo, from Bleach, drinking from a juice box, utterly bored](docs/anime/linters.gif)

*You, reading 300 linter warnings with no explanation at all.*

- 🥋 **Katas**: Java, Spring Boot and Quarkus good-practice rules. Every violation becomes a **lesson** with an
  explanation.
- 🏯 **Architecture classification**: recognizes whether the project is layered,
  package-by-feature, modular monolith, hexagonal, clean, CQRS, event-driven or microservices.
- ⚖️ **Overengineering verdict**: weighs what is in the code against the project's context and
  says whether the architecture is too big (or too small) for the problem.
- 🎖️ **Rank**: from 6th kyū to 1st dan.
- 🌱 **Shoshin mode**: names the concepts already in the code (records, transactions, Repository, Strategy…) and explains each one.
- 🧠 **Optional local LLM**: a model running on your machine explains the verdict. It explains;
  it never decides.
- 🏗️ **CRUD generator**: creates a REST CRUD in the project's architecture and explains the role
  of every file. The generated code passes every kata.
- 🤖 **Claude Code plugin**: checks every file Claude edits and shows it the lessons while it
  writes.

## 💡 Why it exists

- **Linters point, they don't teach.** A warning says something is wrong, but someone learning
  needs the why, the cost and when the rule does not apply. The Shishō writes the whole lesson.
- **Too much architecture is a mistake too.** Plenty of tutorials teach hexagonal or Clean
  Architecture for a three-field CRUD, and no tool asks whether the architecture fits the
  project. The Shishō asks, and answers with facts measured in the code.
- **Deterministic and local.** Same input, same result, so it runs in CI. Your code never leaves
  your machine, not even with the LLM, which runs locally and is optional.

## 🚀 Installation

All you need is **Java 25** ([Temurin](https://adoptium.net), for example). The installer
downloads `shisho.jar` from the latest [release](https://github.com/mauricioandrade/shisho/releases),
checks its checksum and asks for the language:

```bash
bash -c "$(curl -fsSL https://raw.githubusercontent.com/mauricioandrade/shisho/main/install.sh)"
```

The jar goes to `~/.local/share/shisho/` and the `shisho` command to `~/.local/bin/`. If you allow
it, the installer puts that folder on the `PATH` and writes `SHISHO_IDIOMA` (`pt` or `en`) to your
`~/.bashrc` or `~/.zshrc`; running it again updates the version and replaces both lines. The
language applies to everything: reports, sensei guides and the code `shisho crud` generates. To
switch for a single command: `SHISHO_IDIOMA=pt shisho analyze .`.

Rather download it by hand? The release's `shisho.jar` runs with `java -jar shisho.jar analyze <project>`.

The analyzed code may be written in English or in Portuguese: the Shishō recognizes packages such
as `dominio`, `servico` and `porta/entrada` just as well as `domain`, `service` and `port/in`.

![Gojo Satoru, from Jujutsu Kaisen, in chibi form, skipping happily through a field of flowers](docs/anime/install.gif)

*One command, one question, and the Shishō is ready.*

## ⚡ Quick start

Point it at the root of a Maven or Gradle project on Java 21 or later:

| Command | What it does |
|---|---|
| `shisho analyze ./my-project` | lessons, architecture, verdict and rank |
| `shisho analyze --sensei ./my-project` | the same, with the why and the before/after of each lesson |
| `shisho analyze --shoshin ./my-project` | explains the concepts in the code: records, dependency injection, Strategy… |
| `shisho analyze --format html --output r.html ./my-project` | HTML report (or `sarif`, for GitHub) |
| `shisho fix ./my-project` | shows the automatic fixes; `--apply` writes them |
| `shisho crud --arquitetura layered ./my-project Product name:String` | creates a CRUD that explains every file |
| `shisho ask ./my-project "where do I start?"` | a local LLM explains the verdict (needs Ollama) |
| `/plugin install shisho --marketplace mauricioandrade/shisho` | in Claude Code: the Shishō checks every file Claude edits |

`shisho <command> --help` lists every option.

## 🎬 Demo

![Terminal recording: shisho analyze points out four lessons, shisho fix --apply fixes three and the new analysis gives 1st dan](docs/demo/demo-en.gif)

A project with four lessons: the Shishō points out each one and explains why, `fix --apply`
fixes three by itself, and the rank goes from 2nd kyū to 1st dan.

<details>
<summary>The text output, on a bigger project</summary>

```
$ shisho analyze ./clarity-api

師匠 Shishō — analysis of clarity-api  (Java 21)
Architecture: Technical layers (Layered)  (83% of the signature: controller, service, repository, model, dto)

✗ SEVERE  [field-injection]             UserController.java:14
          userService is injected straight into the field. So new creates the class without the
          dependency, the error only shows up as a NullPointerException in use, and a test needs
          Spring or reflection. Receive it through the constructor, in a private final field.
! MEDIUM  [request-body-without-valid]  OrderController.java:27
          @RequestBody CreateOrderRequest without @Valid: the DTO validation annotations are
          ignored, and invalid data reaches the service as if it were valid. Add @Valid before
          @RequestBody.
! MEDIUM  [entity-in-controller]        OrderController.java:31
          Endpoint find() exposes the JPA entity Order in the API. Use request/response records: the
          database schema stops being the API contract, internal fields do not leak and clients
          cannot overwrite them.

Verdict: underengineering. The project outgrew Technical layers (Layered):
  · OrderService has 31 methods and 9 constructor dependencies.
Natural next step: package-by-feature.

Rank: 3rd kyū  (8 lessons: 1 severe, 4 medium, 3 minor)
```

</details>

Each lesson says what is wrong, what goes wrong because of it and how to fix it.

## 🥋 Katas

| Kata | Severity | What it flags | Why it matters |
|---|---|---|---|
| `field-injection` | GRAVE | `@Autowired`/`@Inject` on a field (in Quarkus, only `@Autowired`) | `new` creates an incomplete object, and the test needs Spring |
| `lombok-constructor` | MEDIA | `@RequiredArgsConstructor`/`@AllArgsConstructor` | the dependencies vanish from the code, and the tenth one slips in unnoticed |
| `non-final-dependency` | MEDIA | injected dependency without `final` | any method can replace it after construction |
| `system-out` | MEDIA | `System.out`, `System.err`, `printStackTrace()` | no level or timestamp, and outside the log aggregator |
| `request-body-without-valid` | MEDIA | request body without `@Valid` (`@RequestBody` or a Jakarta REST body) | the DTO's validation is ignored, and invalid data reaches the service |
| `entity-in-controller` | MEDIA | endpoint that receives or returns a JPA `@Entity` | the database becomes the API contract, and internal fields leak |
| `missing-controller-advice` | MEDIA | controllers and no `@RestControllerAdvice` at all (in Quarkus, `ExceptionMapper`) | every endpoint handles (or forgets) its own errors |
| `avoided-dependency` | MEDIA | abandoned, replaced or relocated dependency (Log4j 1, Springfox, Joda-Time... see [`catalog/dependencies`](catalog/dependencies)) | no security fixes, or already replaced by its own project |
| `value-outside-constructor` | LEVE | `@Value` outside a constructor parameter | the value arrives by reflection, and a test using `new` goes without it |
| `dto-not-record` | LEVE | DTO (by package or suffix) that is not a record | hand-written getters, `equals` and `hashCode`, and mutable |
| `transactional-read-only` | LEVE | read method (`find…`, `buscar…`) in a write transaction | Hibernate dirty-checks for nothing |
| `configuration-properties-not-record` | LEVE | `@ConfigurationProperties` on a class with setters | the configuration can change after it is loaded |
| `prefer-configuration-properties` | LEVE | 3 or more `@Value` (or `@ConfigProperty`) with the same prefix in one class | loose properties, with no type or validation in one place |
| `missing-actuator` | LEVE | web application without `spring-boot-starter-actuator` (in Quarkus, `quarkus-smallrye-health`) | with no health check, the orchestrator cannot tell whether the app is alive |
| `missing-springdoc` | INFO | web application without springdoc-openapi (in Quarkus, `quarkus-smallrye-openapi`) | API consumers get no contract and no Swagger UI |
| `prefer-virtual-threads` | INFO | `Executors.newFixedThreadPool`/`newCachedThreadPool` (Java 21) | for I/O, virtual threads need no pool sizing |
| `prefer-scoped-values` | INFO | `ThreadLocal` field (Java 25) | `ScopedValue` does not leak between tasks and needs no `remove()` |
| `quarkus-private-injection` | LEVE | `private` field with `@Inject` or `@ConfigProperty` (Quarkus only) | ArC falls back to reflection, which costs at startup and in the native image |

A Quarkus project is recognized by its build (an `io.quarkus` dependency): the katas use its
equivalents (CDI, Jakarta REST, SmallRye) and explain the lesson with the Quarkus APIs. Why and
how: [ADR 0012](docs/adr/0012-quarkus.md).

The severities are GRAVE (severe), MEDIA (medium), LEVE (minor) and INFO. Suggestions (INFO)
cost no points. With `--sensei`, every lesson comes with the full explanation, the cases where
the flagged code is acceptable and a before/after example.

![Sung Jinwoo, from Solo Leveling, with one eye glowing blue and a half smile](docs/anime/katas.gif)

*Nobody starts at S-rank. Every lesson you fix is experience.*

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

## 🏅 The rank

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

## 📖 Sensei mode: learn and fix

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
| `quarkus-private-injection` | drops `private` from the injected field |

Only the fixed lines change: formatting and comments stay as they were. When the fix is not
safe (a `@Qualifier` that would have to move, a `@Builder` that relies on Lombok's
constructor…), the lesson is left to you, with the sensei mode example. Why JavaParser and not
OpenRewrite: [ADR 0003](docs/adr/0003-autofix.md).

## 🌱 Shoshin mode: the concepts in your code

Sensei mode explains what is wrong. Shoshin (初心, Zen's "beginner's mind") explains what is
**already** in the code: with `--shoshin`, the report names every concept the project shows, where
it is and what it is.

```
$ shisho analyze --shoshin ./shop
...
初心 Concepts in this code (shoshin mode)

Java
  Record  (4 places: CustomerRequest.java:5, CustomerResponse.java:3, ProductRequest.java:8 and 1 more)
    A class made only to carry data. You declare the components in parentheses, and the compiler
    writes the rest: constructor, accessors (`name()`, with no `get`), `equals`, `hashCode` and
    `toString`. The components are `final`: once created, a record does not change.
    [...]

Spring
  Constructor dependency injection  (4 places: CustomerController.java:23, CustomerService.java:14, ...)
  [...]

Jakarta EE
  Transactions (@Transactional)  (8 places: CustomerService.java:9, CustomerService.java:26, ...)
  [...]

Design patterns
  Repository  (2 places: CustomerRepository.java:5, ProductRepository.java:5)
  [...]
```

There are 23 concepts in five groups, in the order a beginner should meet them:

| Group | Concepts |
|---|---|
| Java | record, enum, sealed types, `Optional`, streams |
| Spring | beans, dependency injection, REST controller, error handling, `@ConfigurationProperties` |
| Quarkus | CDI beans and scopes, Jakarta REST resources, Panache, `@ConfigMapping` |
| Jakarta EE | JPA entity, transactions, Bean Validation |
| Design patterns | Repository, Active Record, DTO, Strategy, Builder, Observer |

Run it on a project generated by `shisho crud` to see every piece explained. Shoshin describes, it
does not judge: a recognized Strategy is not praise, and the verdict is what says whether it fits.
The concepts live in [`catalog/concepts`](catalog/concepts), in Markdown, and a test requires each
one to recognize its own example. Why it works this way: [ADR 0009](docs/adr/0009-shoshin.md).

## 📐 Overengineering

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
Architecture: Clean Architecture  (100% of the signature: domain, application, infrastructure, usecase, domain/model, domain/port)

Verdict: overengineering. Clean Architecture costs 7 to 12 classes per feature, and nothing in the
declared context pays for it. It is worth it when: dominio_com_regras_complexas, vida_util_longa,
troca_provavel_de_infra, time_grande.
  · A mapper converts UsuarioJpaEntity into Usuario, which have exactly the same fields.
  · Usuario and the JPA entity UsuarioJpaEntity have exactly the same fields: two copies of one
    structure, plus the mapping between them.
  · 4 use cases just for CRUD operations on Usuario: one use case per method, with no business rule
    to justify each one.
Consider package-by-feature.
```

The context keys (`dominio_com_regras_complexas`, `vida_util_longa`…) are the ones you write in
`shisho.yaml`, so they stay as they are in any language.

When the context justifies the architecture but the code still shows the signals, the verdict is
**"overengineering, for now"**: it names the `shisho.yaml` reason that pays for the cost, shows what
the code does not use yet and does not suggest switching architectures, since the choice was
made.

It works the other way too: a layered project with a 30-method service, or services that depend
on each other, gets an **underengineering** verdict and the next step up.

![Dazai, from Bungou Stray Dogs, stuck upside down in a drum, suffering](docs/anime/overengineering.gif)

*Twelve classes for a CRUD. Getting in was easy.*

What the Shishō knows about each architecture lives in
[`catalog/architectures`](catalog/architectures), in Markdown: when it is worth the cost and
when it is overkill.

## 🧱 From scratch: `shisho crud`

The Spring Initializr sets up the build and the dependencies, but it does not tell you where each
class goes or why. `shisho crud` creates a complete REST CRUD (entity, repository, DTOs, service,
controller and error handling) in the project's architecture, and explains the role of every
file:

```
$ shisho crud --arquitetura package-by-feature ./shop Product name:String price:BigDecimal
師匠 Shishō — CRUD of Product in Package by Feature

+ src/main/java/com/example/shop/product/Product.java
  The JPA entity: the database table seen as an object. Like every class in `product`, it is
  package-private (no `public`): no other feature sees the entity, and features talk to each other
  through the service. [...]

+ src/main/java/com/example/shop/product/ProductService.java
  Where the business rules live and where the transaction starts. The whole class is
  `readOnly = true`, and only the methods that write open a write transaction. [...]
...
Created 8 files. Run shisho analyze to check: the generated code passes every kata.
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
- **Quarkus.** In a Quarkus project (an `io.quarkus` dependency in the build), the same three
  architectures come out the Quarkus way: a Jakarta REST resource, `PanacheRepository`, `@Inject`
  on a package-private field and a `@ServerExceptionMapper` for the 404s. A code.quarkus.io
  project with no code has no package yet, so the first `crud` asks for `--pacote`.

![Joseph Joestar, from JoJo's Bizarre Adventure, running while someone shouts "Run away!"](docs/anime/never-overwrites.gif)

*The file already exists? Nigerundayo! The Shishō backs off without writing anything.*

### Before generating: why?

Hexagonal costs more files than a CRUD usually needs. Before generating it, the Shishō asks for
the reason, and without one it writes nothing:

```
$ shisho crud --arquitetura hexagonal ./shop Product name:String price:BigDecimal
⚠ This CRUD in Hexagonal (Ports & Adapters) creates 12 files, and nothing in the shisho.yaml context
pays for that cost. The architecture is worth it when the project has:
multiplas_integracoes_externas, dominio_com_regras_complexas, troca_provavel_de_infra,
vida_util_longa. For a CRUD without business rules, package-by-feature does the same with fewer
files.

Nothing was written. If the project has one of these reasons, declare it under
contexto.caracteristicas in shisho.yaml: the decision is recorded, and shisho analyze uses it too.
To generate anyway, use --mesmo-assim.
```

![Gintoki, from Gintama, holding a carton of strawberry milk, in disbelief](docs/anime/cost-warning.gif)

*Hexagonal. For a two-field CRUD. Seriously?*

With the reason declared in [`shisho.yaml`](#-overengineering), the CRUD is generated right away,
and the report names the reason that paid for it. `--mesmo-assim` ("anyway") is for when you just
want to see what the architecture looks like. In a project that is already hexagonal, the next
`crud` goes ahead without asking: the decision was already made. Why it works this way:
[ADR 0006](docs/adr/0006-crud-cost-warning.md).

Right after it is generated, `analyze` gives that CRUD **"overengineering, for now"**: the domain is
still a copy of the table, with the same fields as the JPA entity. The verdict changes once the
domain gains rules of its own. There is no clean template: for a CRUD, it would only add layers to
what hexagonal already shows ([ADR 0011](docs/adr/0011-no-clean-crud.md)).

![Dazai, from Bungou Stray Dogs, kneeling in a garden and laughing](docs/anime/cost-paid.gif)

*With the reason declared in `shisho.yaml`, Dazai gets out of the drum.*

A test applies every template to a project like the Initializr's and requires `shisho analyze`
to recognize the architecture and find **no** lesson at all. The code was also checked on a real
Initializr project (Spring Boot 4.1) and on a code.quarkus.io one (Quarkus 3.40): it compiles,
starts and answers. The templates live in [`catalog/scaffolds`](catalog/scaffolds), in Markdown.
Why it works this way: [ADR 0005](docs/adr/0005-crud-scaffolding.md) and, for Quarkus,
[ADR 0014](docs/adr/0014-quarkus-crud.md).

## 🧠 The LLM explains, it does not judge

Everything the Shishō decides is deterministic: same input, same result, ready for CI. The local
LLM (through [Ollama](https://ollama.com)) is **optional and off by default**: only `shisho ask`
talks to it. The model receives the facts and the verdict already computed, plus the catalog's
text about the detected and the suggested architecture, and only writes the explanation:

```
$ shisho ask ./shop-api "why not Clean Architecture here?"
Asking gemma3:4b at http://localhost:11434...
Alright, student. Let's examine this analysis. The core finding is that the current architecture
– Clean Architecture – is being applied in a way that's currently overengineered for this
project. [...]

Specifically, the analysis points to the mapper converting `UsuarioJpaEntity` to `Usuario`, which
duplicates the field structure. Furthermore, the four use cases, each handling a single CRUD
operation on `Usuario`, contribute to this redundancy. [...]

Explanation written by gemma3:4b, which may be wrong. The verdict, the rank and the lessons come
from the deterministic analysis: shisho analyze.
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

## 🤖 In Claude Code

With the Shishō plugin, Claude Code gets checked while it writes. After every edit to a `.java`
file, the Shishō analyzes the project and shows Claude that file's lessons, and Claude fixes them
before moving on. A skill teaches Claude to use the CLI when you ask about architecture,
overengineering or for a CRUD.

```
/plugin install shisho --marketplace mauricioandrade/shisho
```

If you installed with `install.sh`, the plugin finds the jar by itself; if not, it asks where
`shisho.jar` is when you enable it. In a real run, Claude wrote a field `@Autowired`, got the GRAVE lesson and
switched to constructor injection without anyone asking.

![Gojo, Yuji and Nobara, from Jujutsu Kaisen, jumping and high-fiving to celebrate](docs/anime/claude-code.gif)

*Claude writes, the Shishō points, Claude fixes. High five.*

The Shishō only points things out. When a lesson does not apply, the plugin asks Claude to
explain that to you instead of rewriting the code just to silence the kata. Why it works this
way: [ADR 0008](docs/adr/0008-claude-code-plugin.md).

## ☕ Java versions

The Shishō runs on **Java 25** and analyzes projects from **Java 21** on. It reads your project's
version and **only asks for what that version allows**. Anything that only exists in a newer
version becomes a suggestion that costs no points:

```
i INFO    [prefer-scoped-values]  NotificationService.java:11
          When you move to Java 25: Field CURRENT_USER is a ThreadLocal. ...
```

Preview features are never recommended. What the Shishō knows about each version lives in
[`catalog/java-features`](catalog/java-features).

![Super Saiyan Goku, from Dragon Ball Z, turning to the camera with a smile and a thumbs up](docs/anime/java-versions.gif)

*Moved your project to Java 25? The Shishō unlocks the new katas.*

## 📊 Reports and exit codes

Besides the terminal, the report comes out as SARIF (for GitHub code scanning and other tools)
or HTML (a single page, no dependencies, light and dark themes):

```bash
shisho analyze --format sarif --output shisho.sarif ~/projects/my-api
shisho analyze --format html  --output shisho.html  ~/projects/my-api
```

The exit code is designed for CI and does not depend on the format:

| Code | Meaning |
|---|---|
| 0 | no severe lesson |
| 1 | at least one **GRAVE** (severe) lesson |
| 2 | invalid input (missing path or argument, an `--output` that cannot be written, an unknown `SHISHO_IDIOMA`; in `crud`, an architecture nothing pays for) |
| 3 | the project could not be analyzed (e.g. Java below 21, invalid `shisho.yaml`); in `fix --apply`, a file that could not be written; in `ask`, Ollama down or the model not installed |

## 🚦 On GitHub Actions

```yaml
jobs:
  shisho:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      security-events: write   # to upload the SARIF to code scanning
    steps:
      - uses: actions/checkout@v4
      - uses: mauricioandrade/shisho@main
        with:
          path: .                # project to analyze (default: repository root)
          fail-on-grave: true    # fails the job on a GRAVE lesson (default: true)
          upload-sarif: true     # lessons as code scanning alerts (default: true)
          html: shisho.html      # optional: also writes the HTML report
```

Lessons show up as code scanning alerts and pull request annotations; the terminal report goes
to the job summary. The action installs Java 25 and builds the Shishō from its own source. Since
that changes the job's Java, **run it in a job separate** from your project's build.

![Naoya Zen'in, from Jujutsu Kaisen, eyes closed and unmoved, while Choso shouts behind him](docs/anime/github-actions.gif)

*Someone yelling in the channel because the build broke. The Shishō, calmly, had already said so in the PR.*

## 🧰 Development

To work on the Shishō itself, you need **JDK 25** and **Maven 3.9**. If you already have both,
skip straight to `mvn verify`.

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

`target/shisho.jar` is the version you just built: `java -jar target/shisho.jar analyze
<project>`. Before opening a PR, run `mvn verify` and the Shishō on itself (`java -jar
target/shisho.jar analyze .`): the Shishō's own code has to pass its katas.

To use the version you built instead of the installed one, run `./install.sh --from-source` in
the clone.

mise is only a development convenience. The Shishō does not depend on it, and nothing in the
build requires it.

**The README demo** is recorded by [`docs/demo/record.sh`](docs/demo/record.sh), with
[VHS](https://github.com/charmbracelet/vhs) in a Docker container, in both languages. Did the
Shishō's output change? Run the script again. A test makes sure the demo project still tells the
same story.

**Publishing a version** means creating the tag: `git tag v0.1.0 && git push origin v0.1.0`. The
[`release.yml`](.github/workflows/release.yml) workflow builds with that version, runs every test
and publishes `shisho.jar` and its checksum in a GitHub Release, which `install.sh` then
downloads. Why it works this way: [ADR 0010](docs/adr/0010-release.md).

## 📍 Roadmap

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
- [x] **Phase 8**: English and Portuguese, both for the reports and for the analyzed code
  (`SHISHO_IDIOMA`, `install.sh`)
- [x] **Phase 9**: a Claude Code plugin, where the Shishō checks every file Claude edits
- [x] **Phase 10**: shoshin mode (`--shoshin`): the Java, Spring and design pattern concepts that
  appear in the code
- [x] **Phase 11**: a release with the ready-made jar for every tag, and a one-command install
- [x] **Phase 12**: Quarkus: the katas, shoshin and the classification recognize CDI, Jakarta REST
  and Panache
- [x] **Phase 13**: Gradle: `build.gradle` and `build.gradle.kts` read without running the script
  ([ADR 0013](docs/adr/0013-gradle.md))
- [x] **Phase 14**: `shisho crud` in Quarkus projects, in all three architectures
  ([ADR 0014](docs/adr/0014-quarkus-crud.md))
- [ ] **Next**: other stacks, reusing the architecture catalog

![Sung Jinwoo, from Solo Leveling, striking with a dagger among stone statues](docs/anime/roadmap.gif)

*One phase at a time, like clearing a dungeon.*

## 🤝 Contributing

The easiest way to contribute requires no Java at all: **the catalog is Markdown**. A new
architecture, a missing overengineering signal, a clearer explanation, a CRUD template for
another architecture: each one is a PR in `catalog/`. The schema is in
[`catalog/README.md`](catalog/README.md). Every text has a Portuguese and an English version
(`catalog/en/`). If you only write one of them, open the PR anyway: the test shows what is
missing, and someone fills it in.

![Naruto making the Shadow Clone Jutsu hand sign](docs/anime/contributing.gif)

*One dev alone can't cover every kata. We need clones.*

Issues labeled `good first issue` are a good starting point.

## 📄 License

Code under [Apache 2.0](LICENSE). The anime GIFs belong to their creators and studios and are
used for illustration only.

## 🙏 Credits

- [mise](https://github.com/jdx/mise), by Jeff Dickey and contributors (MIT): Java and Maven
  version management during development.
- [JavaParser](https://github.com/javaparser/javaparser) (Apache 2.0 / LGPL): the Java parser
  behind the katas. The choice is in [`docs/adr/0001-parser.md`](docs/adr/0001-parser.md).
- [picocli](https://picocli.info) (Apache 2.0): command line.
- [SnakeYAML](https://bitbucket.org/snakeyaml/snakeyaml) (Apache 2.0): reads the catalog's
  frontmatter and Ollama's responses.
- [Ollama](https://ollama.com) (MIT): server for the optional local LLM; not bundled in the jar.

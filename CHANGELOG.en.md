# Changelog

What changed in each version of the Shishō. A version's section becomes the notes of its GitHub
release, in Portuguese ([CHANGELOG.md](CHANGELOG.md)) and English. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the versions follow
[SemVer](https://semver.org/): while it is 0.x, a new minor version may change behavior.

## [Unreleased]

## [0.3.0] - 2026-10-18

### Heads up

- The new `entity-without-default-constructor` kata is **GRAVE**: a JPA entity without a no-argument
  constructor now gives exit code 1. On a project with lessons that cannot be fixed yet, record a
  baseline (`shisho baseline .`) and CI then fails only on new ones.

### Added

- **JPA katas**: `entity-without-default-constructor` (GRAVE), `eager-collection`,
  `to-one-without-lazy` and `transactional-controller` ([ADR 0021](docs/adr/0021-jpa-katas.md)).
- **Baseline**: `shisho baseline <dir>` records today's lessons in `shisho-baseline.yaml`, and from
  then on `analyze`, the GitHub Action and the Claude Code hook show and fail only on new lessons.
  For adopting the Shishō on an existing project ([ADR 0023](docs/adr/0023-baseline.md)).

### Changed

- **Performance**: files are parsed in parallel, and the analysis is twice as fast on a project of
  4 thousand files and 25–30% faster on one of a few hundred, with the same result
  ([ADR 0022](docs/adr/0022-performance.md)).

### Fixed

- `install.sh` without a terminal (in a script or CI) exited with code 1 and no message at the
  language and PATH prompts. It now takes the defaults, as if the person pressed Enter.

## [0.2.0] - 2026-10-18

### Heads up

- The new `unproxyable-method` kata is **GRAVE**: a `@Transactional` on a `private` method now
  gives exit code 1, and a CI that runs the Shishō may fail where it used to pass.
- Projects on Java 8 to 20 are no longer refused (the exit code used to be 3).

### Added

- **Quarkus**: the katas, shoshin mode and the classification recognize CDI, Jakarta REST and
  Panache; `quarkus-private-injection` kata ([ADR 0012](docs/adr/0012-quarkus.md)).
- **Gradle**: `build.gradle` and `build.gradle.kts` read without running the script, with the
  version catalogs (libraries, bundles and catalogs named in `settings.gradle`)
  ([ADR 0013](docs/adr/0013-gradle.md)).
- **`shisho crud` on Quarkus**, in all three architectures ([ADR 0014](docs/adr/0014-quarkus-crud.md)).
- **Multi-module**: Maven modules (from profiles too) and Gradle ones (with `projectDir`), and
  `buildSrc` convention plugins; each lesson goes to the build that declares it, and the Claude
  Code hook climbs to the root ([ADR 0015](docs/adr/0015-multi-module.md)).
- **Java 8 to 20 and Spring Boot 2**: what needs a newer version becomes a "When you move to Java X"
  suggestion; Spring Boot 2 is recognized by the version the build pins or by `javax.*` imports,
  and `shisho fix` imports `javax.validation.Valid` there
  ([ADR 0017](docs/adr/0017-java-17.md), [ADR 0018](docs/adr/0018-java-8-and-javax.md)).
- **New katas**: `unproxyable-method`, `swallowed-exception` and `entity-with-lombok-data`
  ([ADR 0019](docs/adr/0019-new-katas.md)). `unproxyable-method` also flags a call from inside
  the class itself (`this.save()`), which skips the proxy
  ([ADR 0020](docs/adr/0020-self-invocation.md)).

### Fixed

False positives found by running the Shishō on spring-petclinic, JHipster, buckpal, eladmin and
other real projects ([ADR 0016](docs/adr/0016-real-projects.md)):

- `request-body-without-valid` does not ask for `@Valid` on a DTO with no constraint, nor on a body
  made only of JDK types (`Set<Long>`, `List<String>`) or a `MultipartFile`;
- `lombok-constructor` only looks at beans, including those of custom stereotypes (`@UseCase`);
- `missing-controller-advice` and `missing-springdoc` ignore controllers that only render views;
- `dto-not-record` ignores builders and stateless classes;
- tests in other source sets (`src/integration-test`, `src/testFixtures`…) are not analyzed.

### Changed

- `shisho crud` refuses projects below Java 17 or still on `javax.*` / Spring Boot 2: it generates
  Spring Boot 3 and Quarkus 3 code.

## [0.1.1] - 2026-10-10

### Changed

- The repository was renamed to `shisho`.

## [0.1.0] - 2026-10-10

The first version: Spring Boot katas with a rank, architecture classification and an
overengineering verdict, terminal, HTML and SARIF reports, a GitHub Action, sensei mode with
`shisho fix`, `shisho ask` with a local LLM, `shisho crud`, Portuguese and English, a Claude Code
plugin and shoshin mode.

[Unreleased]: https://github.com/mauricioandrade/shisho/compare/v0.3.0...HEAD
[0.3.0]: https://github.com/mauricioandrade/shisho/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/mauricioandrade/shisho/compare/v0.1.1...v0.2.0
[0.1.1]: https://github.com/mauricioandrade/shisho/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/mauricioandrade/shisho/releases/tag/v0.1.0

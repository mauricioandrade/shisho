---
name: shisho
description: Use the Shishō (師匠) CLI on Java/Spring Boot projects. Use it when the user asks to review a Java or Spring Boot project, check its architecture or whether it is overengineered, explain a Shishō lesson, fix what the katas flag, explain the concepts and design patterns in their code, or create a REST CRUD (entity, repository, DTOs, service, controller) in the project's architecture.
---

# Shishō

The Shishō analyzes Java 21+ / Spring Boot projects deterministically: it applies katas (good
practice rules) and reports lessons, recognizes the architecture, gives an overengineering or
underengineering verdict and a rank. Its judgment is the source of truth; your job is to run it,
explain what it says and act on it with the user.

The jar is at `${user_config.shisho_jar}` (`~` is the user's home). If that path is empty, use
`$SHISHO_JAR`, or the `shisho` command that `install.sh` puts on the PATH; if none works, ask the
user to run `install.sh`. Every command takes
the project root, the directory with `pom.xml` or `build.gradle(.kts)`:

```bash
java -jar "${user_config.shisho_jar}" analyze <project>            # lessons, architecture, verdict, rank
java -jar "${user_config.shisho_jar}" analyze --sensei <project>   # adds why, when it is fine, before/after
java -jar "${user_config.shisho_jar}" analyze --shoshin <project>  # names the concepts in the code, for beginners
java -jar "${user_config.shisho_jar}" fix <project>                # shows the safe automatic fixes as a diff
java -jar "${user_config.shisho_jar}" fix --apply <project>        # writes them
java -jar "${user_config.shisho_jar}" crud [--arquitetura <id>] <project> <Entity> name:Type...
```

Exit codes: 0 no severe lesson, 1 severe lessons, 2 invalid input, 3 the project could not be
analyzed. Output follows `SHISHO_IDIOMA` (`pt`, the default, or `en`).

## Reviewing a project

1. Run `analyze`. For the lessons you will discuss, run `analyze --sensei` and read the guide: it
   says why the kata exists and when the flagged code is acceptable.
2. Report the verdict and the most severe lessons first. Quote the Shishō; do not invent lessons
   it did not report, and do not argue with its verdict: explain it.
3. Offer `fix` for the lessons it can fix mechanically. Show the diff before `--apply`.

## Teaching a beginner

When the user is learning (asks what something in the code is, or what a pattern means), run
`analyze --shoshin`. It names the Java features, Spring concepts and design patterns the project
shows, with where each one is. Start from those places in their own code rather than from a
textbook example.

## Lessons after your own edits

The plugin's hook runs the Shishō after you edit a file and shows you that file's lessons. Fix
the ones that make sense. When a lesson does not apply (the guide's "acceptable when" case),
tell the user why instead of rewriting the code only to silence the kata.

## Creating a CRUD

`crud` creates every file of a REST CRUD in the project's architecture and explains each one.

- Fields are `name:Type` with `String`, `Integer`, `Long`, `Double`, `Boolean`, `BigDecimal`,
  `LocalDate`, `LocalDateTime`, `Instant` or `UUID`. Do not pass `id`: it is generated.
- Without `--arquitetura`, it follows the project's architecture. A project fresh from the
  Spring Initializr has none yet: ask the user, and suggest `package-by-feature` or `layered`.
- `hexagonal` writes nothing unless the `shisho.yaml` context declares a reason that pays for its
  cost. Do not reach for `--mesmo-assim` on your own: show the user the warning, explain the
  trade-off and let them choose between declaring the reason, a simpler architecture or
  `--mesmo-assim`.
- It never overwrites a file. Afterwards, relay the explanations that matter to the user and run
  `analyze`.

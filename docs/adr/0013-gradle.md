# ADR 0013 — Projetos Gradle, lidos sem rodar o Gradle

- **Status:** aceito
- **Data:** 2026-10-15
- **Fase:** 13 (Gradle)

## Contexto

Até aqui o Shishō só lia `pom.xml`. Num projeto Gradle, o `BuildFileReader` voltava vazio: a
análise assumia Java 21, não via nenhuma dependência e, sem elas, não reconhecia o Quarkus
(ADR 0012), não cobrava actuator nem springdoc, não apontava dependências evitadas. O hook do
Claude Code nem achava a raiz do projeto, porque procurava só `pom.xml`.

O Spring Initializr gera Gradle por padrão em boa parte dos tutoriais, e o code.quarkus.io
oferece as duas opções. Deixar Gradle de fora deixava metade dos alunos sem metade das katas.

O `pom.xml` é XML: dá para ler com o parser do JDK e chegar exatamente no que o Maven usaria.
Um `build.gradle(.kts)` é um **programa** (Groovy ou Kotlin). O que ele declara só existe de
verdade depois de executado.

## Decisão

### 1. Ler o script como texto, nunca executá-lo

`GradleBuildFileReader` lê `build.gradle.kts` (ou, se não houver, `build.gradle`) e reconhece
as formas **literais** que o Spring Initializr, o code.quarkus.io e a documentação do Gradle
escrevem:

| O quê | Formas reconhecidas | Precedência |
|---|---|---|
| Versão do Java | `options.release = 21` / `options.release.set(21)` | 1ª |
| | `JavaLanguageVersion.of(21)` (toolchain) | 2ª |
| | `sourceCompatibility = JavaVersion.VERSION_21`, `= '21'`, `= JavaVersion.toVersion(21)` | 3ª |
| Dependências | `implementation("g:a[:v]")`, `implementation 'g:a'`, `group: 'g', name: 'a'`, `group = "g", name = "a"` | — |

A precedência repete a do próprio Gradle: `release` decide o nível de linguagem que o compilador
aceita; sem ele, a versão da toolchain vira o padrão do `sourceCompatibility`.

Só conta o bloco `dependencies { }` **de nível superior**: o de dentro de `buildscript { }` traz
plugins do Gradle, não bibliotecas do projeto. `platform(...)` e `enforcedPlatform(...)` são BOMs
e ficam de fora, como o `dependencyManagement` do Maven. Comentários são removidos antes da
leitura (respeitando strings, para que `'https://...'` não vire comentário), então uma
dependência comentada não conta.

### 2. Maven primeiro

`BuildFileReaders` tenta Maven e depois Gradle e fica com o primeiro que entende o projeto. Um
projeto com os dois arquivos (raro, em geral uma migração pela metade) é lido pelo `pom.xml`.
É a segunda implementação real da porta `BuildFileReader`, então a porta continua do jeito que
estava; o composto mora em `infrastructure/build`, e o `domain` não sabe que existe Gradle.

### 3. O que não é visto

- versão ou dependência calculada pelo script: `JavaLanguageVersion.of(jdkVersion)`, `if`, loops;
- **version catalogs** (`implementation(libs.spring.boot.starter.web)`), que vivem em
  `gradle/libs.versions.toml`;
- multi-módulo (`settings.gradle` com `include`), igual ao Maven hoje;
- `kotlin { jvmToolchain(21) }`, que é configuração de projeto Kotlin.

Nesses casos o Shishō faz o mesmo que faz com um parent remoto no Maven: assume Java 21 e diz
que assumiu. Uma versão literal inválida (`JavaLanguageVersion.of(0)`) é erro de entrada, com a
mesma mensagem do Maven.

O teste `aGradleBuildTeachesTheSameLessonsAsMaven` analisa o `aluno-quarkus-baguncado` com o
`pom.xml` trocado por um `build.gradle.kts` equivalente e exige o mesmo relatório, só com o nome
do arquivo de build trocado.

## Alternativas registradas

- **Gradle Tooling API.** Daria o modelo resolvido de verdade (variáveis, catalogs, módulos). Mas
  executa o script do projeto analisado, que é código não confiável, baixa o Gradle e as
  dependências pela rede, leva segundos e quebra o "mesma entrada → mesma saída". Uma CLI que
  roda em CI e no hook do Claude Code não pode depender disso.
- **Parser de Groovy e de Kotlin.** Duas gramáticas e duas dependências pesadas para chegar numa
  AST que ainda precisaria ser interpretada. As expressões regulares cobrem as formas que os
  geradores escrevem, que é o que um aluno tem.
- **Ler `gradle/libs.versions.toml`.** Útil, mas TOML pediria uma dependência nova (o JDK não
  tem parser) ou um parser próprio. Fica para quando um aluno real chegar com catalog.

## 📚 Estudo — declarar versus executar

Maven e Gradle resolvem o mesmo problema com filosofias opostas. O Maven é **declarativo**: o
`pom.xml` é um documento, e qualquer ferramenta lê o que ele diz sem rodar nada. O Gradle é
**programável**: o build é código, e isso dá liberdade (um `if` para escolher a dependência, uma
função para configurar dez módulos iguais), mas cobra um preço — para saber o que o build declara,
alguém precisa executá-lo.

É o mesmo dilema de qualquer analisador estático. A análise de verdade (rodar) é exata e
perigosa; a análise sobre o texto é segura e aproximada. O Shishō escolhe o texto e assume o
"aproximado" de forma explícita: quando não reconhece a versão, ele não chuta, diz que assumiu o
mínimo. Um build escrito com valores literais é lido inteiro, e esse é mais um motivo, além da
legibilidade, para deixar o `build.gradle` o mais declarativo possível.

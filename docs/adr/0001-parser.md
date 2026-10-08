# ADR 0001 — Parser de Java: JavaParser

- **Status:** aceito
- **Data:** 2026-10-08
- **Fase:** 1 (MVP)

## Contexto

O Shishō precisa transformar código-fonte Java 21+ em um `ProjectModel` (classes, anotações,
campos, métodos, imports, pacote, com linha de cada elemento) para aplicar as katas. A escolha
do parser fica atrás da porta `SourceParser`, mas define o quanto é fácil escrever katas e o
quanto o Shishō acompanha versões novas da linguagem.

Candidatos:

1. **JavaParser** (`com.github.javaparser:javaparser-core`, + `javaparser-symbol-solver-core`
   para resolução de tipos).
2. **API de árvore do javac** (`com.sun.source.tree` / `com.sun.source.util`, módulo
   `jdk.compiler`), usando `JavacTask.parse()`.

**Critério decisivo:** parsear sem erro um fixture com sintaxe Java 25 — `import module`
(JEP 511), corpo de construtor flexível (JEP 513) e variáveis/padrões sem nome `_` (JEP 456).

## Spike

Fixture usado (resumo): `import module java.base;`, campo com `@Autowired`, `sealed interface`
com `record`s, construtor com `if (...) throw` e atribuição **antes** de `super()`, `switch`
com record pattern e `var _`, `for (String _ : items)`, `catch (NumberFormatException _)` e
lambda `_ -> {}`. Executado em Temurin 25.0.4.

| Verificação | JavaParser 3.28.2 | javac (JDK 25.0.4) |
|---|---|---|
| Fixture com nível 25 | ✅ 0 problemas | ✅ 0 diagnósticos |
| Mesmo fixture com nível 21 | ✅ rejeita `import module`, construtor flexível e `_` | ⚠️ rejeita `import module` e `_`; construtor flexível só é checado na fase de atribuição, não no `parse()` |
| `import module` na AST | `ImportDeclaration.isModule()` | `ImportTree.isModule()` |
| `super()` tardio na AST | `ExplicitConstructorInvocationStmt` como 3º statement | idem, como `MethodInvocationTree` |
| Tempo do parse (1 arquivo, JVM fria) | ~165 ms | ~280 ms |
| Dependências | `javaparser-core` (~1,5 MB); com symbol solver ~6 MB (traz Guava e Javassist) | nenhuma, mas exige `jdk.compiler` em tempo de execução (JDK completo, não JRE/jlink mínimo) |

Os dois passam no critério decisivo. O desempate foi pelo custo de escrever e manter katas.

## Decisão

Usar **JavaParser** (`javaparser-core`), com o nível de linguagem configurado **explicitamente**
a partir da versão do projeto analisado (`ParserConfiguration.setLanguageLevel`).

O **JavaSymbolSolver não entra agora.** As katas da Fase 1 são puramente sintáticas. Ele só
será adicionado quando uma kata real precisar de resolução de tipos (provavelmente na Fase 2,
"entidade JPA exposta em controller") e, mesmo assim, só se um índice simples do próprio
`ProjectModel` não bastar.

### Por quê

- **AST mais ergonômica para katas.** `findAll(FieldDeclaration.class)`, `getParentNode()`,
  `getBegin().line` em qualquer nó. No javac, posição exige `SourcePositions` + `LineMap` e
  navegação para cima só existe via `TreePath` durante o scan. Katas são o código que mais vai
  crescer no projeto; elas devem ser curtas.
- **Validação por nível de linguagem completa no parse.** Com o nível do projeto, o JavaParser
  aponta uso de recurso acima da versão declarada (inclusive construtor flexível). Isso conversa
  com a consciência de versão do §6.
- **Não amarra o runtime.** Funciona em qualquer JVM 25, sem exigir `jdk.compiler` — mantém
  aberta a opção de distribuir uma imagem jlink enxuta.
- **Caminho para resolução de tipos tolerante.** O symbol solver resolve tipos com classpath
  parcial; o `JavacTask.analyze()` gera erro para cada símbolo do Spring ausente.

### Alternativa registrada: javac Tree API

Seria a escolha "biblioteca padrão" e tem uma vantagem real: suporta toda sintaxe nova no mesmo
dia em que o Shishō rodar no JDK novo, sem esperar release de terceiro. Se o JavaParser atrasar
muito em uma versão futura da linguagem, este ADR deve ser revisto — e a porta `SourceParser`
existe justamente para que a troca fique restrita a `infrastructure/parser`.

## Consequências

- Nova dependência: `com.github.javaparser:javaparser-core:3.28.2` (versão pinada).
- **Armadilha:** `new ParserConfiguration()` e o `StaticJavaParser` usam `JAVA_11` por padrão
  e falham em records, sealed, `switch` com padrões etc. O adaptador **nunca** usa o
  `StaticJavaParser`; sempre cria `JavaParser` com nível explícito.
- O fixture do spike vira teste de regressão do adaptador (`src/test/resources/fixtures/`).
- Risco: suporte a Java 26+ depende do ritmo de releases do JavaParser (o `JAVA_26` já existe
  no branch principal em out/2026).

## 📚 Estudo — parser, AST e resolução de símbolos

Um **parser** transforma texto em uma **AST** (*Abstract Syntax Tree*): uma árvore em que cada
nó é uma construção da linguagem — declaração de classe, campo, anotação, chamada de método. Uma
kata como `field-injection` é só uma pergunta feita à árvore: "existe um nó `FieldDeclaration`
com a anotação `@Autowired`?".

Parsear é diferente de **resolver símbolos**. A AST sabe que um campo é do tipo `Usuario`, mas
não sabe *qual* `Usuario` (do pacote `domain`? uma `@Entity`?). Responder isso exige olhar os
imports, os outros arquivos e as bibliotecas do classpath — é o que o javac faz na fase de
**atribuição** e o que o JavaSymbolSolver emula. É mais lento, mais frágil (classpath
incompleto) e mais caro de manter, por isso o Shishō só paga esse custo quando uma kata
realmente precisar.

Repare também no detalhe da tabela: o javac não acusa o construtor flexível no `parse()`. Em um
compilador, cada regra mora em uma fase diferente (léxica, sintática, semântica). "Parseou sem
erro" não significa "é um programa válido nessa versão do Java".

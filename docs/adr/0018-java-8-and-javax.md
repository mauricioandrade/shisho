# ADR 0018 — Java 8 e o namespace `javax`

- **Status:** aceito
- **Data:** 2026-10-18
- **Fase:** 17b (Java 8 e Spring Boot 2)

## Contexto

O [ADR 0017](0017-java-17.md) baixou o piso para o Java 17 e deixou o resto para depois. O motivo
estava registrado lá: abaixo do 17, quase todo projeto é Spring Boot 2, e o Spring Boot 2 usa os
pacotes `javax.*` do Java EE. O Spring Boot 3 usa os mesmos pacotes renomeados para `jakarta.*`. O
Shishō analisa bem os dois, porque as katas reconhecem anotações pelo nome simples (`@Entity` é
`@Entity` nos dois). Mas três coisas que ele **gera ou recomenda** supunham `jakarta`:

- o `@Valid` que o `shisho fix` importa (`jakarta.validation.Valid`);
- os templates do `shisho crud`, que usam records e `jakarta.*`;
- a lição da `missing-controller-advice`, que recomenda `ProblemDetail`, uma classe que só existe
  no Spring 6 (Boot 3).

Dois projetos da calibração ([ADR 0016](0016-real-projects.md)) são Java 11 e Spring Boot 2: o
spring-boot-realworld-example-app e o ddd-by-examples/library.

## Decisão

### 1. O piso é o Java 8

`JavaVersion.MINIMUM` passou a 8, o mínimo do Spring Boot 2. Records entraram no catálogo
`java-features` (desde o 16), e as duas katas que sugerem record (`dto-not-record`,
`configuration-properties-not-record`) os declaram. Num projeto 11, elas viram "Ao migrar para o
Java 16: …", do mesmo jeito que a de virtual threads vira "Ao migrar para o Java 21" num projeto
17. O `ASSUMED` continua 21.

### 2. Um projeto `javax` é reconhecido pelos imports

O build de um projeto Spring Boot não mostra a versão do framework (ela vem do parent ou do plugin,
que o Shishō não lê). O código mostra. Um projeto é `javax` quando importa algum dos pacotes que o
Jakarta EE renomeou (`persistence`, `validation`, `servlet`, `ws.rs`, `inject`, `enterprise`,
`transaction`) com `javax.` e nenhum com `jakarta.`. Um projeto no meio da migração, com os dois,
já conta como `jakarta`. O `javax.annotation` fica de fora, porque o `@Nullable` do JSR 305 mora
lá em projetos de qualquer idade.

A regra é o `JavaxProjects`, em `infrastructure/kata`, e só as katas a usam:

- a `request-body-without-valid` põe na lição, como segundo argumento, o `@Valid` a importar. O
  fixer usa esse nome e não decide nada sozinho;
- a `missing-controller-advice` usa a mensagem `kata.missing-controller-advice.lesson.spring-boot-2`,
  que pede o formato do RFC 9457 sem falar em `ProblemDetail`.

### 3. O `crud` recusa o que não compilaria

O `crud` continua gerando só código Boot 3 / Quarkus 3. Gerar código legado iria contra o que o
Shishō ensina, e manter dois conjuntos de templates dobraria o catálogo. Ele recusa antes de gravar
qualquer coisa, com uma mensagem que diz o que migrar:

- abaixo de `GenerateCrud.MINIMUM_JAVA` (17): `crud.error.java-version`;
- quando o projeto importa um pacote que o template declara em `recusa_imports` (campo novo do
  schema, documentado em `catalog/README.md`): `crud.error.refused-import`.

O `recusa_imports` fica no template, e não numa regra em Java, porque o que não combina com o
código gerado é uma propriedade desse código. Assim, a camada `application` continua sem saber o
que é `javax`: ela só compara os imports do projeto com uma lista do catálogo, do mesmo jeito que
compara o build com `dependencias`.

## Alternativas registradas

- **Ler a versão do Spring Boot no build** (o `<parent>` do Maven, o plugin do Gradle). É mais
  exato: o `library` é Boot 2.2 e não importa nenhum pacote renomeado (usa JDBC puro), então ainda
  recebe a lição com `ProblemDetail`. Mas o `BuildInfo` não guarda versões, e passar a guardar
  mexe nos dois leitores de build. Fica como próximo passo, se aparecerem mais casos.
- **Templates `javax` para o `crud`.** Dobrariam o catálogo para gerar código de um framework sem
  suporte desde 2023.
- **Lição própria para o `@ConstructorBinding` do Boot 2** na `configuration-properties-not-record`.
  A anotação só é obrigatória num record do Boot 2.2 ao 2.5; do 2.6 em diante, um record com um
  construtor dispensa. Sem saber a versão do Boot, uma lição própria erraria na maior parte dos
  projetos. A nuance foi para o guia da kata.

O que continua de fora: o Spring Boot 1.x, que o Shishō analisa (Java 8), mas cujas diferenças não
foram estudadas, e a alternativa ao Springfox que a `avoided-dependency` sugere
(`springdoc-openapi-starter-webmvc-ui`), que é a do Boot 3.

## 📚 Estudo — de `javax` para `jakarta`

Em 2017 a Oracle entregou o Java EE à Eclipse Foundation, mas não a marca "Java" nem o direito de
mexer nos pacotes `javax.*`. A especificação virou **Jakarta EE**, e a versão 9 (2020) fez a mudança
mais simples e mais trabalhosa possível: trocou `javax.` por `jakarta.` em todos os pacotes, sem
mudar mais nada. `javax.persistence.Entity` virou `jakarta.persistence.Entity`, com o mesmo
comportamento.

Para um framework, isso é uma quebra total, porque código compilado contra um pacote não enxerga o
outro. O Spring esperou uma versão maior: o Spring Boot 3 (2022) é `jakarta`, e o 2 é `javax`. O
Quarkus fez o mesmo no 3. Por isso uma migração para o Boot 3 começa quase sempre por um
"buscar e substituir" nos imports, e ferramentas como o OpenRewrite automatizam essa etapa.

Também por isso um import diz mais do que parece. `import javax.persistence.Entity` não diz só
"isto é uma entidade": diz "este projeto ainda não migrou", e o Shishō usa esse sinal.

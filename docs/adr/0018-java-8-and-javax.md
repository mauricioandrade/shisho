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

### 2. Um projeto `javax` é reconhecido pelo build ou pelos imports

O `BuildInfo` ganhou `platformVersions`: as versões que o build fixa para uma plataforma inteira,
por groupId. No Maven, o `<parent>` e os BOMs importados em `<dependencyManagement>`; no Gradle, os
plugins com versão (`id("org.springframework.boot") version "2.6.3"`), os `platform(...)` e o
`classpath` de um `buildscript` antigo. Só valem versões literais ou propriedades do próprio arquivo.
O domínio não sabe o que é Spring Boot: quem pergunta pelo `org.springframework.boot` (ou pelo
`io.quarkus.platform`) é a infraestrutura. Abaixo da versão 3, o projeto é `javax`.

Quando o build não fixa versão (um parent corporativo, que herda a do Spring Boot), o código
responde. Um projeto é `javax` quando importa algum dos pacotes que o
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
  schema, documentado em `catalog/README.md`): `crud.error.refused-import`;
- quando o build fixa uma plataforma abaixo da que o template declara em `plataforma_minima`
  (`org.springframework.boot:3`): `crud.error.platform-version`. Isso pega um Spring Boot 2.7 em
  Java 17 que ainda não importa nada de `javax`.

O `recusa_imports` e o `plataforma_minima` ficam no template, e não numa regra em Java, porque o que não combina com o
código gerado é uma propriedade desse código. Assim, a camada `application` continua sem saber o
que é `javax`: ela só compara os imports do projeto com uma lista do catálogo, do mesmo jeito que
compara o build com `dependencias`.

## Alternativas registradas

- **Só os imports, sem ler o build.** Foi a primeira versão desta decisão. O `library` é Boot 2.2
  e não importa nenhum pacote renomeado (usa JDBC puro), e recebia a lição com `ProblemDetail`. Ler
  a versão no build resolveu esse caso; os imports ficaram como resposta quando o build não diz.
- **Guardar a versão de toda dependência**, em vez de só a das plataformas. Nenhuma kata precisa,
  e a versão de uma dependência num projeto Spring quase nunca está escrita: quem a decide é o BOM.
- **Templates `javax` para o `crud`.** Dobrariam o catálogo para gerar código de um framework sem
  suporte desde 2023.
- **Lição própria para o `@ConstructorBinding` do Boot 2** na `configuration-properties-not-record`.
  A anotação só é obrigatória num record do Boot 2.2 ao 2.5; do 2.6 em diante, um record com um
  construtor dispensa. Sem saber a versão do Boot, uma lição própria erraria na maior parte dos
  projetos. A nuance foi para o guia da kata.

O que continua de fora: o Spring Boot 1.x, que o Shishō analisa (Java 8), mas cujas diferenças não
foram estudadas; versões de plataforma vindas de variáveis (`"$springBootVersion"`) ou do
`[plugins]` do version catalog. A alternativa ao Springfox que a `avoided-dependency` sugere passou
a citar o artefato do Boot 2 (`springdoc-openapi-ui`) ao lado do Boot 3.

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

# ADR 0015 — Projetos multi-módulo

- **Status:** aceito
- **Data:** 2026-10-15
- **Fase:** 15 (multi-módulo)

## Contexto

Até aqui o Shishō lia um build só, o da raiz. O parser já achava as classes de todos os módulos (ele
percorre a árvore inteira e pula `src/test` em qualquer profundidade), mas o build ficava pela
metade. Num multi-módulo típico, o `pom.xml` da raiz só tem `<packaging>pom</packaging>` e a lista
dos módulos, e as dependências moram em `app/pom.xml`. Sem elas, o Shishō não via que o projeto era
web nem que era Quarkus, e não cobrava actuator ou springdoc. Num Gradle cuja raiz tem só o
`settings.gradle.kts`, não achava build nenhum.

O hook do Claude Code tinha o problema oposto. Ao editar `app/src/.../Controller.java`, ele subia
até o primeiro diretório com build, `app/`, e analisava o módulo sozinho, sem o resto da
arquitetura.

## Decisão

### 1. O build da raiz conhece os seus módulos

`BuildInfo` ganhou `modules`: o build de cada módulo, com o seu próprio arquivo
(`app/pom.xml`, `dominio/build.gradle.kts`) e as dependências que **ele** declara. Os módulos de
um módulo entram na mesma lista, já achatada.

- **Maven:** `<modules><module>` e, do Maven 4, `<subprojects><subproject>`, lidos
  recursivamente. Os módulos de um `<profile>` entram também, ativo ou não: o parser analisa o
  código deles de qualquer jeito, então o build precisa ser conhecido. Já as dependências
  declaradas dentro do profile ficam de fora, porque só valem quando ele está ativo. Um módulo fora do projeto (`../outro`), sem `pom.xml` ou já lido (dois poms que
  se listam) é pulado: o projeto analisado não é confiável, e um ciclo não pode travar a análise.
- **Gradle:** os `include(...)` do `settings.gradle(.kts)`, com o caminho do projeto virando
  diretório (`":app:web"` → `app/web`). A raiz pode não ter script próprio; aí o build é o
  `settings.gradle.kts`. O version catalog é o da raiz, e vale para todos os módulos. O
  `includeBuild` é outro build, não um módulo, e fica de fora.

### 2. Perguntas sobre o projeto olham todos os módulos; a lição aponta para o módulo

| Pergunta | Onde olha |
|---|---|
| É Quarkus? É web? Tem actuator? Tem Bean Validation? | `allDependencies()`: a raiz e todos os módulos |
| Em que arquivo fica a lição? | `fileDeclaring(dependência)`: o build que a declara |
| Qual a versão do Java? | `lowestJavaVersion()`: a menor que algum build declara |

A união resolve de graça a herança do Maven: um actuator declarado no `<dependencies>` do pom pai
vale para o módulo `app`, e a kata não acusa. Também evita o falso positivo do caminho contrário:
um módulo `dominio` sem nada web não é uma "aplicação web sem actuator".

A lição de `missing-actuator` vai para o arquivo que declara o starter web, porque é ali que o
actuator deve entrar; a de `avoided-dependency`, para o arquivo que declara a dependência
evitada. Sem isso, as duas apontariam para o `pom.xml` da raiz, que não tem nenhuma das duas.

A versão é a menor porque o código de todos os módulos é analisado com um nível de linguagem só.
Se um módulo compila em Java 21, nenhuma kata pode sugerir ali um recurso do 25.

### 3. O hook sobe até a raiz

`ClaudeHookCommand` acha o primeiro diretório com build acima do arquivo editado e continua subindo
enquanto o diretório de cima também tiver build (`pom.xml`, `build.gradle(.kts)` ou
`settings.gradle(.kts)`). Num multi-módulo, isso dá a raiz; num projeto comum, dá o mesmo diretório
de antes.

## Alternativas registradas

- **Juntar tudo num build só**, com as dependências de todos os módulos e o arquivo da raiz. Era a
  mudança menor, mas todas as lições de build apontariam para um `pom.xml` que não declara nada.
- **Analisar cada módulo separado**, com a sua própria graduação. A arquitetura é do projeto: num
  hexagonal dividido em módulos, `dominio/` sozinho não é hexagonal nem nada. E a herança do Maven
  teria que ser resolvida à mão em cada módulo.
- **Ler só os profiles com `<activeByDefault>`**, imitando o Maven sem `-P`. Mas o código dos
  outros módulos seria analisado do mesmo jeito, só que sem build: um controller num módulo de
  profile pareceria de um projeto sem starter web.
- **No hook, conferir se o build de cima lista o módulo**, em vez de só ver se tem build. Seria mais
  exato, mas precisaria ler os dois builds a cada edição. Um diretório com build logo acima de outro
  com build é, na prática, um multi-módulo.

O que continua de fora: `project(":x").projectDir = file(...)`
no Gradle e dependências declaradas em convention plugins do `buildSrc`. As de `subprojects { }` e
`allprojects { }` passaram a ser lidas como do build da raiz ([ADR 0013](0013-gradle.md)).

## 📚 Estudo — agregação e herança

O Maven mistura duas ideias que vale separar. **Agregação** é o `<modules>`: "quando eu rodar o
build aqui, rode também nestes diretórios". **Herança** é o `<parent>`: "comece com o que o outro
pom declara". Num multi-módulo típico o pom da raiz faz os dois papéis ao mesmo tempo, mas eles são
independentes: um módulo pode ter outro pai, e um pai pode não listar módulo nenhum (é o caso do
`spring-boot-starter-parent`).

O Gradle separa as duas coisas desde o começo. O `settings.gradle` diz quais projetos fazem parte do
build (agregação), e o que é comum entre eles vai para um *convention plugin*, um script reaproveitado
que cada módulo aplica. É composição em vez de herança, a mesma troca que se faz no código Java.

O Shishō lê a agregação dos dois e aproxima a herança pela união das dependências. É uma
aproximação consciente: dá a resposta certa no layout que quase todo mundo usa, sem precisar
reimplementar o modelo efetivo do Maven.

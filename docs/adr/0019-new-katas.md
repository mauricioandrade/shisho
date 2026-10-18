# ADR 0019 — Três katas novas, e uma descartada

- **Status:** aceito
- **Data:** 2026-10-18
- **Fase:** 18 (katas novas)

## Contexto

As katas da Fase 1 e 2 cobrem injeção, DTOs, validação e o build. Faltavam erros comuns que o
código mostra e que custam caro em produção: anotações que o Spring ignora sem avisar, exceções
engolidas e o Lombok mal usado em entidades JPA. Quatro candidatas foram escolhidas e conferidas nos
projetos reais da calibração ([ADR 0016](0016-real-projects.md)) antes de entrar.

## Decisão

### O modelo ganhou modificadores de método e blocos `catch`

`ClassUnit.Method` passou a ter `modifiers`, como o próprio `ClassUnit` já tinha, e o `ClassUnit`
ganhou `catches`: os tipos capturados (vários num multi-catch), o nome da variável, se o corpo não
tem instrução e se tem comentário. Os blocos dentro de lambdas e classes anônimas pertencem ao tipo
que os contém, a mesma regra das chamadas (`invocations`).

### As katas

| Kata | Severidade | Regra |
|---|---|---|
| `unproxyable-method` | GRAVE | `@Transactional`, `@Async`, `@Cacheable`, `@CachePut`, `@CacheEvict`, `@Caching` ou `@Retryable` num método `private`, `static` ou `final`; no Spring Boot 2 ([ADR 0018](0018-java-8-and-javax.md)), em qualquer método que não seja `public` |
| `swallowed-exception` | MEDIA | `catch` sem instrução e sem comentário, a não ser que a variável se chame `ignored`, `ignore`, `expected` ou `_` |
| `entity-with-lombok-data` | MEDIA | `@Entity` com `@Data` ou com `@EqualsAndHashCode` sem atributos |

A `unproxyable-method` é GRAVE porque o erro é de correção, não de estilo: o código roda sem
transação e nada avisa. Ela não roda no Quarkus, que desde a versão 3 faz o build falhar com uma
anotação de interceptor num método `private`
([Migration Guide 3.0](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.0)) e
intercepta métodos `static`. A chamada de dentro da própria classe (`this.salvar()`), que também
escapa do proxy, fica de fora: o modelo não diz de que método parte cada chamada.

A `swallowed-exception` aceitava, na primeira versão, só o nome da variável como sinal de "é de
propósito". Nos projetos reais apareceram quatro `catch` vazios, e os quatro explicavam o motivo
num comentário (`//idempotent operation`, `// ignore, store not ready yet`). Os quatro eram falsos
positivos, então o comentário passou a contar. O Shishō tinha um caso desses, no `AnalyzeCommand`,
que ganhou também o nome `ignored`.

A `entity-with-lombok-data` achou as duas entidades JPA do buckpal, as duas com `@Data`.

## Alternativas registradas

- **`optional-field-or-parameter`**, que acusaria `Optional` como campo ou parâmetro. Foi
  descartada pelo dogfooding: o próprio Shishō tem uns 15 parâmetros `Optional` e usa `Optional`
  em componentes de record no domínio inteiro, de forma consistente. A orientação do JDK é usar
  `Optional` principalmente como retorno, mas tratá-lo como valor também é um estilo defensável. A
  regra do CLAUDE.md manda corrigir o código ou questionar a kata, e aqui quem estava errada era a
  kata: ela ensinaria uma opinião como se fosse boa prática.
- **Exigir um comentário e também o nome `ignored`** na `swallowed-exception`. Seria mais rígido
  do que o código real que se conferiu, e sem ganho: os dois sinais dizem a mesma coisa a quem lê.
- **Correção automática** para as três. Nenhuma tem uma troca mecânica segura: tornar um método
  `public` muda a API da classe, o `catch` vazio pede uma decisão (logar, relançar ou ignorar) e o
  `equals` de uma entidade depende de qual é a chave dela.

## 📚 Estudo — proxy

Um **proxy** é um objeto que se põe no lugar de outro e intercepta as chamadas para fazer algo
antes e depois. O Spring usa proxies para quase tudo que vem por anotação: transação, cache,
execução assíncrona, segurança (`@PreAuthorize`). Quando um bean tem `@Transactional`, o que o
Spring injeta nos outros não é o seu objeto, é uma subclasse gerada em tempo de execução que
abre a transação e só então chama o seu método.

Disso saem as duas regras que a `unproxyable-method` ensina. A primeira: o proxy só intercepta o
que uma subclasse consegue sobrescrever, e por isso `private`, `static` e `final` ficam de fora. A
segunda, que a kata não vê: o proxy só intercepta chamadas **que chegam de fora**. Quando um método
do bean chama outro com `this.metodo()`, a chamada vai direto para o objeto, sem passar pelo
proxy. É por isso que a solução comum dos dois casos é a mesma: pôr o método anotado em outro bean,
para que a chamada atravesse o proxy.

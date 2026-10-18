# ADR 0020 — Auto-invocação

- **Status:** aceito
- **Data:** 2026-10-18
- **Fase:** 19 (auto-invocação)

## Contexto

A `unproxyable-method` ([ADR 0019](0019-new-katas.md)) acusava as anotações que o proxy do Spring
não alcança por causa do modificador do método. Ela deixava de fora o caso mais comum: um método do
bean chamando outro método anotado **da mesma classe**. A chamada `salvar()` (ou `this.salvar()`)
vai direto ao objeto, sem passar pelo proxy, e a anotação é ignorada nessa chamada. Para ver isso,
o modelo precisava dizer de que método parte cada chamada, e não dizia.

## Decisão

### O modelo

`ClassUnit.Invocation` ganhou dois campos:

- `caller`: o método do tipo em que a chamada está escrita. Uma chamada numa lambda ou numa classe
  anônima dentro de `fechar()` conta como de `fechar()`, a mesma regra que as chamadas já seguiam
  para decidir a que tipo pertencem. Fica vazio num construtor ou num inicializador;
- `arguments`: quantos argumentos a chamada passa, ou `UNKNOWN_ARGUMENTS` numa referência de método
  (`this::salvar`). Isso separa as sobrecargas com o mesmo nome.

### A regra

Uma chamada sem alvo, ou com alvo `this`, para um método da mesma classe com `@Transactional`,
`@Async` ou as anotações de cache, perde a anotação. A lição sai quando essa perda muda alguma
coisa:

| Anotação no método chamado | Acusa quando |
|---|---|
| `@Async`, `@Cacheable`, `@CachePut`, `@CacheEvict`, `@Caching`, `@Retryable` | sempre: a chamada roda síncrona, sem cache, sem nova tentativa |
| `@Transactional` | quem chama não está numa transação (nem o método, nem a classe têm `@Transactional`), ou o método chamado declara uma `propagation` própria, como `REQUIRES_NEW` |
| qualquer uma, numa chamada que sai de um construtor | sempre: ainda não existe proxy |

Fica quieta quando não dá para saber qual método é chamado: se o nome e o número de argumentos
apontam para mais de um método e algum deles não tem a anotação, ou se o método é `private`,
`static` ou `final`, que já tem a lição da primeira regra. O Quarkus continua fora, porque o ArC
intercepta a auto-invocação.

### O que os projetos mostraram

Nenhum dos projetos reais da calibração ([ADR 0016](0016-real-projects.md)) tem uma auto-invocação
que perca a anotação, nem o `macrozheng/mall` e o `elunez/eladmin`, dois projetos grandes que
entraram agora. O JHipster põe `@Transactional` na classe, e a regra o reconhece. O único caso
achado foi o do próprio fixture `aluno-baguncado`: `contarPorPagina()`, sem transação, chama
`contar()`, que é `@Transactional`. O fixture passou a ter um GRAVE a mais, que o `shisho fix` não
corrige.

O eladmin achou outro falso positivo, na `request-body-without-valid`: onze endpoints recebem
`@RequestBody Set<Long>`, e a kata pedia `@Valid` em cada um. Um corpo feito só de tipos do JDK
(valores, datas, coleções e arrays deles), de um `MultipartFile` ou de um `JsonNode` não tem o que
validar, e passou a ser ignorado.

## Alternativas registradas

- **Acusar toda auto-invocação de `@Transactional`**, mesmo de um método que já está em transação.
  A chamada continuaria na mesma transação, então nada se perde: seria uma lição sobre um problema
  que não acontece.
- **Resolver a sobrecarga pelos tipos dos argumentos**, e não pela quantidade. Exigiria o tipo de
  cada expressão, ou seja, um resolvedor de símbolos (o JavaSymbolSolver, deixado de fora no
  [ADR 0001](0001-parser.md)). A quantidade resolve quase todos os casos, e o que sobra fica quieto.

## 📚 Estudo — por que o `this` não passa pelo proxy

Quando o Spring injeta um bean com `@Transactional` em outro, ele entrega o proxy, um objeto que
envolve o bean de verdade. Quem chama `pedidoService.fechar()` de fora chama o proxy, que abre a
transação e chama o `fechar()` do objeto real. Dentro do `fechar()`, o `this` é o **objeto real**,
não o proxy. O proxy sabe do objeto, mas o objeto não sabe do proxy. Por isso `this.salvar()`, ou só
`salvar()`, que é a mesma coisa, chama o método direto, sem ninguém no meio para abrir a transação.

As saídas comuns:

1. **Mover o método para outro bean** e injetá-lo. A chamada passa a atravessar o proxy desse
   outro bean. É a mais limpa, e quase sempre revela uma responsabilidade que merecia classe própria.
2. **Pôr a anotação em quem chama.** No `@Transactional`, se `fechar()` abre a transação, o
   `salvar()` roda dentro dela.
3. **Injetar o próprio bean em si mesmo** (`@Lazy` num campo do próprio tipo) ou usar
   `AopContext.currentProxy()`. Funcionam, mas escondem o problema em vez de resolver o desenho.

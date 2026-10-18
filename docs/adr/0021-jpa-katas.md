# ADR 0021 — Katas de JPA

- **Status:** aceito
- **Data:** 2026-10-18
- **Fase:** 20 (katas de JPA)

## Contexto

As katas de JPA até aqui olhavam a fronteira da API (`entity-in-controller`) e o Lombok
(`entity-with-lombok-data`). Os erros de persistência que mais custam em produção estão no
mapeamento: o que é carregado junto, quando, e se o provedor consegue criar a entidade. Antes de
escrever as regras, cada candidata foi contada no corpus de projetos reais
([ADR 0016](0016-real-projects.md), mais o `macrozheng/mall` e o `elunez/eladmin`), para não criar uma
kata que não acha nada nem uma que acha tudo.

## Decisão

| Kata | Severidade | Regra | No corpus |
|---|---|---|---|
| `entity-without-default-constructor` | GRAVE | `@Entity` ou `@Embeddable` com construtores declarados, nenhum sem parâmetros, sem `@NoArgsConstructor` | nenhum: é um erro que derruba a primeira consulta |
| `eager-collection` | MEDIA | `@OneToMany`/`@ManyToMany` com `fetch = EAGER` | petclinic (3), microservices (2), eladmin (3), um quickstart |
| `to-one-without-lazy` | LEVE | `@ManyToOne`/`@OneToOne` sem `fetch` (o padrão é EAGER), fora o lado inverso de um `@OneToOne(mappedBy = ...)` | petclinic, microservices, eladmin, quatro quickstarts |
| `transactional-controller` | LEVE | `@Transactional` num `@Controller`/`@RestController`, na classe ou num método | JHipster (4) |

As duas de fetch leem o mapeamento nos campos e nos getters (acesso por propriedade), e entendem
`FetchType.EAGER` e o import estático `EAGER`. Um `fetch = FetchType.EAGER` explícito num
relacionamento para um é aceito: a kata existe para o padrão que ninguém escolheu, e um `EAGER`
escrito é uma escolha visível. Na coleção, o `EAGER` sempre é escrito, porque o padrão já é `LAZY`.

A `transactional-controller` é LEVE porque, num CRUD sem camada de service, é um desenho consciente.
O JHipster a gera assim quando se escolhe não ter service. Ela não roda nos resources Jakarta REST,
porque no Quarkus um resource `@Transactional` é o idioma dos próprios guias.

A `entity-without-default-constructor` ignora records: o Hibernate 6.2 mapeia um record como
`@Embeddable` pelo construtor canônico.

## Alternativas registradas

- **Uma kata só para os dois tipos de fetch.** As duas falhas têm pesos diferentes: um `EAGER` numa
  coleção multiplica linhas e é uma escolha errada escrita no código, e o `@ManyToOne` sem `fetch` é
  um padrão herdado que quase todo projeto tem. Juntas, teriam de ter uma severidade só.
- **Acusar também o `EAGER` explícito no relacionamento para um.** Tiraria do aluno uma saída
  legítima, a de dizer "eu quero isto carregado sempre".
- **Detectar o N+1 de verdade**, procurando o acesso à coleção dentro de um laço. Exigiria saber o
  tipo de cada expressão, o mesmo resolvedor de símbolos deixado de fora no
  [ADR 0001](0001-parser.md).

## 📚 Estudo — o N+1

O **N+1** é o erro de desempenho mais comum com JPA. Uma consulta busca N entidades, e depois, para
cada uma, o Hibernate faz mais uma consulta para buscar o relacionamento: 1 + N idas ao banco, onde
uma bastava. Com cem pedidos, são 101 consultas. Em desenvolvimento, com dez registros, ninguém
nota. Em produção, a página fica lenta.

O `EAGER` não resolve, só muda o momento: o relacionamento vem sempre, inclusive quando não é usado,
e numa consulta JPQL o Hibernate continua buscando cada um separado. A solução é deixar tudo `LAZY` e
dizer, **em cada consulta**, o que ela precisa trazer junto:

```java
@Query("select p from Pedido p join fetch p.cliente where p.status = :status")
List<Pedido> buscarComCliente(Status status);
```

ou, no Spring Data, `@EntityGraph(attributePaths = "cliente")` no método do repositório. Assim, quem
lista pedidos com o cliente faz uma consulta só, e quem só conta pedidos não carrega cliente nenhum.

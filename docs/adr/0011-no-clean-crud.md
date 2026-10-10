# ADR 0011 — Sem CRUD clean, e um veredito que entende o contexto

- **Status:** aceito
- **Data:** 2026-10-09
- **Fase:** 7 (gerador de CRUD), revisão

## Contexto

O ADR 0006 deixou o template clean "para quando houver pedido". Ao avaliá-lo, apareceu um
conflito com o próprio Shishō:

- o catálogo proíbe o `domain` do clean de importar `jakarta.persistence`, então o template
  precisaria de um `Produto` no domínio e de uma `ProdutoJpaEntity` com os mesmos campos;
- `entidade_de_dominio_identica_a_entidade_jpa` é um sinal de overengineering do clean, e o
  detector acusaria exatamente esse par;
- qualquer sinal medido dava "overengineering", e o veredito sugeria a arquitetura mais simples
  mesmo quando o `shisho.yaml` declarava um motivo para ela.

O `crud` diria "✓ Clean Architecture se paga neste projeto" e, logo depois, o `analyze` diria
"Veredito: overengineering… Considere package-by-feature" sobre o código que ele mesmo gerou.

O hexagonal gerava a mesma cópia (`Produto` e `ProdutoEntity`) e passava ileso só porque a lista
dele não tinha esse sinal: o custo existia nos dois e só era medido num.

## Decisão

### 1. Sem template clean

Clean e hexagonal são a mesma família no catálogo (`isolamento-de-dominio`). Para um CRUD, o
clean só acrescenta camadas ao que o hexagonal já demonstra. Num projeto clean, o `crud`
continua respondendo que não há template e lista os disponíveis.

### 2. O hexagonal mede a cópia

`entidade_de_dominio_identica_a_entidade_jpa` entra nos sinais de overengineering do hexagonal.
Os dois medem o mesmo custo, e um CRUD hexagonal recém-gerado mostra esse sinal no `analyze`.
É verdade: enquanto o domínio só carrega os campos da tabela, ele é uma cópia.

### 3. Overengineering "por enquanto"

Quando o contexto declarado justifica a arquitetura e mesmo assim há sinais medidos, o veredito
continua sendo overengineering, mas diz isso de outro jeito:

```
Veredito: overengineering, por enquanto. O contexto declarado justifica a arquitetura Hexagonal
(Ports & Adapters) (dominio_com_regras_complexas), mas o código ainda não usa o que ela oferece:
  · Produto e a entidade JPA ProdutoEntity têm exatamente os mesmos campos: duas cópias da mesma
    estrutura, mais o mapeamento entre elas.
Esses sinais somem quando o domínio ganhar regras próprias. Se elas não vierem, o contexto do
shisho.yaml está otimista.
```

Não sugere a arquitetura mais simples, porque quem declarou o contexto já escolheu, nem o `ask`
a sugere ao LLM. O `Verdict.Overengineering` passa a carregar as chaves que justificam
(`justifiedBy`). O "✓ se paga" do `crud` avisa a mesma coisa antes: o CRUD gerado ainda não tem
regra de negócio, e o `analyze` vai apontar o custo até o domínio ganhar as suas.

## Alternativas registradas

- **Burlar o detector,** chamando a classe JPA de `ProdutoRegistro`. Seria enganar a própria régua.
- **Gerar o clean e só avisar.** O aviso não desfaz a contradição entre o `crud` e o `analyze`.
- **Ignorar os sinais quando o contexto justifica.** Esconderia um fato medido; o contexto pode
  estar otimista, e o veredito é o lugar de dizer isso.

## Consequências

- O `crud` tem três templates: layered, package-by-feature e hexagonal.
- Um projeto hexagonal ou clean de verdade, com regras no domínio, não muda de veredito: o
  sinal só aparece quando o domínio e a entidade JPA têm exatamente os mesmos campos.

## 📚 Estudo — fato e contexto

O veredito junta duas coisas de naturezas diferentes. O contexto é uma intenção: "este domínio
vai ter regras complexas". O sinal é um fato: "hoje, o domínio é uma cópia da tabela". Quando os
dois discordam, nenhum anula o outro. A intenção explica por que a estrutura existe; o fato
mostra que ela ainda não está sendo usada. Um bom revisor diz as duas coisas, e é isso que o
"por enquanto" faz.

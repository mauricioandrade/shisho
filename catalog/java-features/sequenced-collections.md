---
id: sequenced-collections
nome: Sequenced collections
desde: 21
status: final
jep: 431
---

## O que é

`List`, `Deque`, `LinkedHashSet` e `LinkedHashMap` ganham uma interface comum para coleções com
ordem: `getFirst()`, `getLast()`, `addFirst()`, `reversed()`.

## Exemplo

```java
Pedido ultimo = pedidos.getLast();          // antes: pedidos.get(pedidos.size() - 1)
List<Pedido> recentes = pedidos.reversed();
```

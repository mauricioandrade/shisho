---
id: stream-gatherers
nome: Stream gatherers
desde: 24
status: final
jep: 485
---

## O que é

Operações intermediárias customizadas em streams, via `Stream.gather`. A JDK já traz algumas em
`Gatherers`, como janelas fixas e deslizantes.

## Exemplo

```java
List<List<Pedido>> lotes = pedidos.stream()
        .gather(Gatherers.windowFixed(100))
        .toList();
```

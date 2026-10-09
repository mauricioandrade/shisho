---
id: record-patterns
nome: Record patterns
desde: 21
status: final
jep: 440
---

## O que é

Desmonta um record direto no `instanceof` ou no `switch`, sem chamar os acessores um a um.

## Exemplo

```java
if (evento instanceof PedidoCriado(var id, var cliente)) {
    notificar(cliente, id);
}
```

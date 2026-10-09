---
id: structured-concurrency
nome: Concorrência estruturada
desde: 25
status: preview
jep: 505
---

## O que é

Trata um grupo de tarefas concorrentes como uma unidade: se uma falha, as outras são canceladas,
e nenhuma sobrevive ao bloco que as criou. Ainda em preview no Java 25: o Shishō não sugere
recursos em preview.

## Exemplo

```java
try (var escopo = StructuredTaskScope.open()) {
    var cliente = escopo.fork(() -> buscarCliente(id));
    var pedidos = escopo.fork(() -> buscarPedidos(id));
    escopo.join();
    return new Resumo(cliente.get(), pedidos.get());
}
```

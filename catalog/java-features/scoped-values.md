---
id: scoped-values
nome: Scoped values
desde: 25
status: final
jep: 506
---

## O que é

Um valor imutável compartilhado com tudo que um trecho de código chama, inclusive em outras
threads, e que some quando o trecho termina. Substitui `ThreadLocal` para contexto de requisição:
não vaza entre tarefas, não precisa de `remove()` e é barato com virtual threads.

## Exemplo

```java
private static final ScopedValue<Usuario> USUARIO_ATUAL = ScopedValue.newInstance();

ScopedValue.where(USUARIO_ATUAL, usuario).run(() -> processar(pedido));
```

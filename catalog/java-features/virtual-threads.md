---
id: virtual-threads
nome: Virtual threads
desde: 21
status: final
jep: 444
---

## O que é

Threads leves, gerenciadas pela JVM e não pelo sistema operacional. Bloquear uma virtual thread
(esperando HTTP, banco, arquivo) é barato: dá para ter uma por tarefa, às centenas de milhares,
sem pool. No Spring Boot 3.2+, `spring.threads.virtual.enabled=true` liga para o servidor web.

## Exemplo

```java
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    pedidos.forEach(pedido -> executor.submit(() -> notificar(pedido)));
}
```

---
id: prefer-virtual-threads
---

## Antes

```java
@Service
public class NotificacaoService {

    private final ExecutorService executor = Executors.newFixedThreadPool(20);

    public void notificarTodos(List<Pedido> pedidos) {
        pedidos.forEach(pedido -> executor.submit(() -> enviarEmail(pedido)));
    }
}
```

## Depois

Enviar e-mail é esperar a rede: com virtual threads, cada envio ganha a sua, sem dimensionar
pool. Para cálculo pesado (CPU), o pool fixo do tamanho dos núcleos continua o certo.

```java
@Service
public class NotificacaoService {

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public void notificarTodos(List<Pedido> pedidos) {
        pedidos.forEach(pedido -> executor.submit(() -> enviarEmail(pedido)));
    }
}
```

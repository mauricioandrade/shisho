---
id: lombok-constructor
---

## Antes

```java
@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidos;
    private final Clock relogio;
}
```

## Depois

```java
@Service
public class PedidoService {

    private final PedidoRepository pedidos;
    private final Clock relogio;

    public PedidoService(PedidoRepository pedidos, Clock relogio) {
        this.pedidos = pedidos;
        this.relogio = relogio;
    }
}
```

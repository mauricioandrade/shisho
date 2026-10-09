---
id: non-final-dependency
---

## Antes

```java
@Service
public class PedidoService {

    private PedidoRepository pedidos;

    public PedidoService(PedidoRepository pedidos) {
        this.pedidos = pedidos;
    }
}
```

## Depois

```java
@Service
public class PedidoService {

    private final PedidoRepository pedidos;

    public PedidoService(PedidoRepository pedidos) {
        this.pedidos = pedidos;
    }
}
```

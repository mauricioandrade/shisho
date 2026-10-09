---
id: field-injection
---

## Antes

```java
@Service
public class PedidoService {

    @Autowired
    private PedidoRepository pedidos;
}
```

## Depois

O construtor declara a dependência; `final` garante que ela não muda. Com um único
construtor, o Spring injeta sem precisar de `@Autowired`, e o teste instancia a classe com
`new PedidoService(fake)`.

```java
@Service
public class PedidoService {

    private final PedidoRepository pedidos;

    public PedidoService(PedidoRepository pedidos) {
        this.pedidos = pedidos;
    }
}
```

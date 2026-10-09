---
id: transactional-read-only
---

## Antes

```java
@Service
public class PedidoService {

    @Transactional
    public List<Pedido> listarPorCliente(String cliente) {
        return pedidos.findByCliente(cliente);
    }
}
```

## Depois

Um padrão comum é marcar a classe inteira como leitura e só os métodos que escrevem com
`@Transactional`.

```java
@Service
@Transactional(readOnly = true)
public class PedidoService {

    public List<Pedido> listarPorCliente(String cliente) {
        return pedidos.findByCliente(cliente);
    }

    @Transactional
    public Pedido criar(Pedido pedido) {
        return pedidos.save(pedido);
    }
}
```

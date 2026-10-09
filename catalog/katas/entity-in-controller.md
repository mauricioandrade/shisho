---
id: entity-in-controller
---

## Antes

```java
@Entity
class Pedido {
    @Id private Long id;
    private String cliente;
    private String observacaoInterna;
}

@RestController
class PedidoController {

    @GetMapping("/pedidos/{id}")
    Pedido buscar(@PathVariable Long id) {
        return pedidos.buscar(id);
    }
}
```

## Depois

O record de resposta escolhe o que sai na API. Renomear uma coluna ou adicionar um campo
interno à entidade não muda mais o contrato.

```java
@Entity
class Pedido {
    @Id private Long id;
    private String cliente;
    private String observacaoInterna;
}

record PedidoResponse(Long id, String cliente) {

    static PedidoResponse de(Pedido pedido) {
        return new PedidoResponse(pedido.getId(), pedido.getCliente());
    }
}

@RestController
class PedidoController {

    @GetMapping("/pedidos/{id}")
    PedidoResponse buscar(@PathVariable Long id) {
        return PedidoResponse.de(pedidos.buscar(id));
    }
}
```

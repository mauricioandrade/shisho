---
id: request-body-without-valid
---

## Antes

```java
@RestController
class PedidoController {

    @PostMapping("/pedidos")
    PedidoResponse criar(@RequestBody CriarPedidoRequest request) {
        return pedidos.criar(request);
    }
}
```

## Depois

```java
@RestController
class PedidoController {

    @PostMapping("/pedidos")
    PedidoResponse criar(@Valid @RequestBody CriarPedidoRequest request) {
        return pedidos.criar(request);
    }
}
```

---
id: missing-controller-advice
---

## Antes

```java
@RestController
class PedidoController {

    @GetMapping("/pedidos/{id}")
    PedidoResponse buscar(@PathVariable Long id) {
        return pedidos.buscar(id);
    }
}
```

## Depois

Um só lugar traduz exceções em respostas HTTP, no formato padrão `application/problem+json`.

```java
@RestController
class PedidoController {

    @GetMapping("/pedidos/{id}")
    PedidoResponse buscar(@PathVariable Long id) {
        return pedidos.buscar(id);
    }
}

@RestControllerAdvice
class TratadorDeErros {

    @ExceptionHandler(PedidoNaoEncontradoException.class)
    ProblemDetail naoEncontrado(PedidoNaoEncontradoException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }
}
```

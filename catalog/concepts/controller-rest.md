---
id: controller-rest
nome: Controller REST
grupo: spring
ordem: 30
aparece_em:
  - anotacao: RestController
referencias:
  - https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller.html
---

## O que é

A porta de entrada HTTP da aplicação. Cada método anotado com `@GetMapping`, `@PostMapping` e
afins atende uma rota, e o Spring converte o JSON da requisição em objetos Java e o retorno do
método em JSON de volta.

Um bom controller só traduz: lê a requisição, chama o service e escolhe o status da resposta. A
regra de negócio mora no service, onde dá para testá-la sem HTTP.

## Exemplo

```java
@RestController
@RequestMapping("/pedidos")
class PedidoController {

    private final PedidoService service;

    PedidoController(PedidoService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    PedidoResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }
}
```

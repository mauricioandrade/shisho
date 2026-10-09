---
id: missing-controller-advice
referencias:
  - https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html
  - https://www.rfc-editor.org/rfc/rfc9457
---

## Por quê

Sem um ponto central, cada controller decide sozinho como responder a um erro: um devolve
`500` com a pilha da exceção, outro um JSON próprio, outro esquece. Quem consome a API precisa
tratar formatos diferentes, e detalhes internos podem vazar na resposta.

Um `@RestControllerAdvice` traduz exceções em respostas HTTP num lugar só. Desde o Spring 6, o
formato padrão é o `ProblemDetail` da RFC 9457 (`application/problem+json`), e estender
`ResponseEntityExceptionHandler` já trata as exceções do próprio Spring MVC, como as de
validação.

## Quando é aceitável

Numa aplicação com poucos endpoints, quando o tratamento padrão basta: com
`spring.mvc.problemdetails.enabled=true`, o Spring Boot já responde as exceções do Spring MVC
como `ProblemDetail`. O advice passa a valer a pena quando há exceções do domínio que precisam
virar status HTTP específicos.

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

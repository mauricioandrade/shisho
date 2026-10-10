---
id: tratamento-de-erros
nome: Tratamento centralizado de erros
grupo: spring
aparece_em:
  - anotacao: RestControllerAdvice
  - anotacao: ExceptionHandler
referencias:
  - https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html
  - https://www.rfc-editor.org/rfc/rfc9457
---

## O que é

Um lugar só que transforma exceções em respostas HTTP. Um `@ExceptionHandler` diz "quando esta
exceção escapar de um controller, responda assim"; num `@RestControllerAdvice`, ele vale para
todos os controllers.

O formato padrão é o ProblemDetail (RFC 9457): um JSON com `status`, `title` e `detail` que todo
cliente sabe ler. Os controllers ficam livres de try/catch.

## Exemplo

```java
@RestControllerAdvice
class TratadorDeErros {

    @ExceptionHandler(PedidoInexistenteException.class)
    ProblemDetail inexistente(PedidoInexistenteException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }
}
```

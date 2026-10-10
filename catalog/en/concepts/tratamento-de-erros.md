---
id: tratamento-de-erros
nome: Centralized error handling
---

## What it is

A single place that turns exceptions into HTTP responses. An `@ExceptionHandler` says "when this
exception escapes a controller, answer like this"; inside a `@RestControllerAdvice`, it applies
to every controller.

The standard format is ProblemDetail (RFC 9457): a JSON with `status`, `title` and `detail` that
any client knows how to read. The controllers stay free of try/catch.

## Example

```java
@RestControllerAdvice
class ErrorHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    ProblemDetail notFound(OrderNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }
}
```

---
id: bean-validation
nome: Bean Validation
grupo: spring
ordem: 60
aparece_em:
  - anotacao: Valid
  - anotacao: NotBlank
  - anotacao: NotNull
  - anotacao: Positive
  - anotacao: Size
  - anotacao: Email
referencias:
  - https://beanvalidation.org/
  - https://docs.spring.io/spring-framework/reference/core/validation/beanvalidation.html
---

## O que é

Regras de validação declaradas como anotações nos campos: `@NotBlank` (texto não vazio),
`@NotNull`, `@Positive`, `@Email`, `@Size`. A regra fica junto do dado, e não espalhada em `if`s
pelo código.

Elas só são checadas quando alguém pede. Num controller, quem pede é o `@Valid` antes do
`@RequestBody`: um corpo inválido vira 400 antes de o método rodar.

## Exemplo

```java
record CriarPedidoRequest(@NotBlank String cliente, @Positive int quantidade) {
}

@RestController
class PedidoController {

    @PostMapping("/pedidos")
    PedidoResponse criar(@Valid @RequestBody CriarPedidoRequest request) {
        return service.criar(request);
    }
}
```

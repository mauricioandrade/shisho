---
id: request-body-without-valid
referencias:
  - https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html
  - https://docs.spring.io/spring-boot/reference/io/validation.html
  - https://quarkus.io/guides/validation#rest-end-point-validation
---

## Por quê

`@NotBlank` e `@Positive` no DTO são só metadados: nada os verifica até alguém pedir. Num
`@RequestBody`, quem pede é o `@Valid` (ou o `@Validated` do Spring). Sem ele, o JSON é
convertido e entregue ao método com os campos inválidos, e as anotações dão uma falsa sensação
de segurança.

Com `@Valid`, um corpo inválido vira `MethodArgumentNotValidException` antes de o método rodar,
e o Spring responde 400. A regra fica declarada no DTO, e o método só trata do caso válido.
Atenção: sem o `spring-boot-starter-validation` no classpath, `@Valid` também não faz nada.

No Quarkus, com Jakarta REST, não existe `@RequestBody`: o corpo é o parâmetro sem anotação de um
método `@POST` ou `@PUT`, e a regra é a mesma, `criar(@Valid CriarPedidoRequest pedido)`. Com a
extensão `quarkus-hibernate-validator`, um corpo inválido vira 400 antes de o método rodar.

## Quando é aceitável

Quando a validação é feita de propósito em outro lugar, como num validador de domínio que
precisa consultar o banco; nesse caso, deixe isso claro no código. DTOs sem nenhuma anotação de
validação a kata já ignora.

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

---
id: injecao-de-dependencia
nome: Injeção de dependência pelo construtor
grupo: spring
ordem: 20
aparece_em:
  - detector: injecao_por_construtor
referencias:
  - https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html
---

## O que é

A classe não cria aquilo de que precisa: ela recebe pronto, pelo construtor. Quem cria e entrega
é o Spring, que procura um bean de cada tipo pedido. Com um único construtor, nem é preciso
`@Autowired`.

O ganho é desacoplamento: a classe depende de um tipo, não de como ele é construído. Num teste,
você passa um objeto falso no lugar do real com um simples `new`, sem subir o Spring.

## Exemplo

```java
@Service
class FechamentoDePedido {

    private final PedidoRepository pedidos;
    private final CalculoDeFrete frete;

    FechamentoDePedido(PedidoRepository pedidos, CalculoDeFrete frete) {
        this.pedidos = pedidos;
        this.frete = frete;
    }
}
```

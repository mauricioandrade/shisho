---
id: non-final-dependency
referencias:
  - https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html
  - https://docs.oracle.com/javase/specs/jls/se25/html/jls-17.html#jls-17.5
  - https://quarkus.io/guides/cdi-reference#simplified-constructor-injection
---

## Por quê

Com `final`, o compilador garante duas coisas: a dependência é atribuída exatamente uma vez,
no construtor, e ninguém a troca depois. Sem `final`, um setter ou um método esquecido pode
substituir o repositório no meio da vida do bean, que é um singleton compartilhado por todas as
requisições.

Há também uma garantia de concorrência. O modelo de memória do Java assegura que qualquer thread
vê os campos `final` já inicializados depois que o construtor termina (JLS 17.5). Num singleton
usado por várias threads ao mesmo tempo, é uma preocupação a menos.

## Quando é aceitável

Quase nunca, se o campo vem do construtor. Quando uma dependência precisa mesmo mudar depois
da construção, como num componente reconfigurado em tempo de execução via JMX (o exemplo que a
documentação do Spring dá para injeção por setter), ela não vem pelo construtor, e esta kata não
a acusa.

No Quarkus, a kata vale igual para beans CDI (`@ApplicationScoped`, `@Singleton`...) e resources
`@Path` que recebem dependências pelo construtor: o ArC aceita campos `final` e gera sozinho o
construtor sem argumentos de que o proxy precisa. Campos com `@Inject` não vêm do construtor e
ficam fora desta kata.

## Antes

```java
@Service
public class PedidoService {

    private PedidoRepository pedidos;

    public PedidoService(PedidoRepository pedidos) {
        this.pedidos = pedidos;
    }
}
```

## Depois

```java
@Service
public class PedidoService {

    private final PedidoRepository pedidos;

    public PedidoService(PedidoRepository pedidos) {
        this.pedidos = pedidos;
    }
}
```

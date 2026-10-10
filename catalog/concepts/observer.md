---
id: observer
nome: Observer (eventos)
grupo: padrao
ordem: 50
aparece_em:
  - anotacao: EventListener
  - anotacao: TransactionalEventListener
  - chamada: publishEvent
referencias:
  - https://refactoring.guru/design-patterns/observer
  - https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html#context-functionality-events
---

## O que é

Quem faz algo publica um evento ("pedido pago"), e quem se interessa reage a ele, sem que um
conheça o outro. O service de pedidos não precisa saber que existe e-mail, nota fiscal ou
estoque: cada um escuta o evento que importa.

No Spring, `publishEvent` publica e `@EventListener` escuta. Com `@TransactionalEventListener`, a
reação só roda depois do commit, para não mandar e-mail de um pedido que não foi gravado.

## Exemplo

```java
record PedidoPago(Long pedidoId) {
}

@Component
class EnvioDeRecibo {

    @TransactionalEventListener
    void quandoPago(PedidoPago evento) {
        enviarRecibo(evento.pedidoId());
    }
}
```

---
id: strategy
nome: Strategy
grupo: padrao
aparece_em:
  - detector: estrategia
referencias:
  - https://refactoring.guru/design-patterns/strategy
---

## O que é

Várias formas de fazer a mesma coisa, cada uma numa classe, todas atrás da mesma interface. Quem
usa conhece só a interface e não precisa de um `if`/`switch` para escolher o comportamento:
recebe a implementação certa, muitas vezes injetada pelo Spring.

Acrescentar um comportamento novo é escrever mais uma classe, sem mexer nas outras. Com uma
implementação só, a interface ainda não é um Strategy: é uma indireção esperando um motivo.

## Exemplo

```java
interface CalculoDeFrete {
    BigDecimal calcular(Pedido pedido);
}

class FreteExpresso implements CalculoDeFrete {
    public BigDecimal calcular(Pedido pedido) {
        return pedido.peso().multiply(new BigDecimal("5"));
    }
}

class FreteEconomico implements CalculoDeFrete {
    public BigDecimal calcular(Pedido pedido) {
        return pedido.peso().multiply(new BigDecimal("2"));
    }
}
```

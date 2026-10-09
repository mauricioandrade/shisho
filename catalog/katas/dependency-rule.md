---
id: dependency-rule
referencias:
  - https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html
  - https://alistair.cockburn.us/hexagonal-architecture/
---

## Por quê

A regra de dependência é o que dá sentido a arquiteturas como hexagonal e clean: o domínio não
conhece os detalhes (banco, web, frameworks), e as dependências apontam para dentro. Assim, as
regras de negócio são testadas sem infraestrutura, e a infraestrutura muda sem tocar no núcleo.

Uma violação não quebra nada hoje; o custo aparece aos poucos. Cada import para fora é um
acoplamento que torna a separação de pacotes decorativa: a estrutura promete um isolamento que o
código não cumpre.

## Quando é aceitável

Se a arquitetura não traz benefício ao projeto (um CRUD simples organizado em hexagonal), a
saída pode ser simplificar a arquitetura, e não criar portas e adaptadores só para respeitar a
regra: veja o veredito de overengineering. Uma violação isolada, consciente e documentada também
pode ser um preço aceitável.

## Antes

O domínio conhece o detalhe de persistência: trocar o JPA por outra coisa obriga a mexer no
núcleo.

```java
package com.loja.domain;

import com.loja.infrastructure.persistence.PedidoJpaRepository;

public class FechamentoDePedido {

    private final PedidoJpaRepository pedidos;
}
```

## Depois

O domínio declara do que precisa (uma porta); a infraestrutura implementa. A dependência
aponta para dentro.

```java
package com.loja.domain;

public interface Pedidos {

    Pedido buscar(PedidoId id);
}

public class FechamentoDePedido {

    private final Pedidos pedidos;
}
```

---
id: dependency-rule
---

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

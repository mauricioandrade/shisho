---
id: value-outside-constructor
---

## Antes

```java
@Service
public class PedidoService {

    @Value("${loja.limite-de-itens}")
    private int limiteDeItens;
}
```

## Depois

```java
@Service
public class PedidoService {

    private final int limiteDeItens;

    public PedidoService(@Value("${loja.limite-de-itens}") int limiteDeItens) {
        this.limiteDeItens = limiteDeItens;
    }
}
```

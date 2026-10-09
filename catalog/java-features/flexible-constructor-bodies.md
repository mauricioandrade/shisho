---
id: flexible-constructor-bodies
nome: Corpo de construtor flexível
desde: 25
status: final
jep: 513
---

## O que é

Código antes de `super(...)` ou `this(...)`: validar argumentos e preparar valores sem método
estático auxiliar.

## Exemplo

```java
Pedido(String cliente) {
    if (cliente.isBlank()) {
        throw new IllegalArgumentException("cliente obrigatório");
    }
    super();
    this.cliente = cliente;
}
```

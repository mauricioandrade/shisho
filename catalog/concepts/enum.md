---
id: enum
nome: Enum
grupo: java
ordem: 20
aparece_em:
  - tipo: enum
referencias:
  - https://docs.oracle.com/javase/tutorial/java/javaOO/enum.html
---

## O que é

Um tipo com um conjunto fixo de valores, como os status de um pedido. Em vez de uma `String`
que aceita qualquer coisa (e um erro de digitação), o compilador só deixa usar os valores
declarados, e o `switch` avisa quando falta um caso.

Um enum é uma classe de verdade: pode ter campos, construtor e métodos, o que deixa o
comportamento de cada valor junto dele.

## Exemplo

```java
enum StatusDoPedido {
    ABERTO, PAGO, ENVIADO, CANCELADO;

    boolean podeCancelar() {
        return this == ABERTO || this == PAGO;
    }
}
```

---
id: sealed
nome: Tipos selados (sealed)
grupo: java
aparece_em:
  - modificador: sealed
referencias:
  - https://openjdk.org/jeps/409
---

## O que é

Uma interface ou classe `sealed` diz exatamente quem pode implementá-la ou estendê-la. A
hierarquia fica fechada: ninguém de fora acrescenta um caso novo sem que você saiba.

O ganho aparece no `switch`: com uma hierarquia selada, o compilador sabe todos os casos e acusa
o que faltou, sem precisar de um `default` que esconde esquecimentos. Desde o Java 17 (JEP 409).

## Exemplo

```java
sealed interface Pagamento permits Pix, Cartao {
}

record Pix(String chave) implements Pagamento {
}

record Cartao(String numero, int parcelas) implements Pagamento {
}
```

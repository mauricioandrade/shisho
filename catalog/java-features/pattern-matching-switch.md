---
id: pattern-matching-switch
nome: Pattern matching para switch
desde: 21
status: final
jep: 441
---

## O que é

`switch` sobre tipos, com guardas (`when`) e checagem de exaustividade quando a hierarquia é
`sealed`: esquecer um caso vira erro de compilação, não um `else` silencioso.

## Exemplo

```java
String descricao = switch (pagamento) {
    case Pix pix -> "Pix " + pix.chave();
    case Cartao cartao when cartao.parcelas() > 1 -> cartao.parcelas() + "x no cartão";
    case Cartao _ -> "cartão à vista";
};
```

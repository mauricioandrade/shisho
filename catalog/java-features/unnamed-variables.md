---
id: unnamed-variables
nome: Variáveis e padrões sem nome
desde: 22
status: final
jep: 456
---

## O que é

`_` no lugar de uma variável ou componente que não é usado: deixa explícito que o valor é
ignorado de propósito.

## Exemplo

```java
try {
    return Integer.parseInt(texto);
} catch (NumberFormatException _) {
    return 0;
}
```

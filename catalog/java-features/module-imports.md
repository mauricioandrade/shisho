---
id: module-imports
nome: Importação de módulos
desde: 25
status: final
jep: 511
---

## O que é

`import module java.base;` importa todos os pacotes exportados por um módulo de uma vez. Útil
em scripts e protótipos; em código de produção, imports explícitos continuam mais claros.

## Exemplo

```java
import module java.base;
```

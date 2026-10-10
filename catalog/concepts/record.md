---
id: record
nome: Record
grupo: java
ordem: 10
aparece_em:
  - tipo: record
referencias:
  - https://openjdk.org/jeps/395
  - https://docs.oracle.com/en/java/javase/21/language/records.html
---

## O que é

Uma classe feita só para carregar dados. Você declara os componentes entre parênteses, e o
compilador escreve o resto: construtor, acessores (`nome()`, sem o `get`), `equals`, `hashCode` e
`toString`. Os componentes são `final`: depois de criado, um record não muda.

É o formato natural de DTOs, respostas de API e valores que andam juntos, como um período com
início e fim. Desde o Java 16 (JEP 395).

## Exemplo

```java
record Periodo(LocalDate inicio, LocalDate fim) {

    boolean contem(LocalDate dia) {
        return !dia.isBefore(inicio) && !dia.isAfter(fim);
    }
}
```

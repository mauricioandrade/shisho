---
id: config-mapping
nome: Configuração no Quarkus (@ConfigMapping)
grupo: quarkus
ordem: 40
aparece_em:
  - anotacao: ConfigMapping
  - anotacao: ConfigProperty
referencias:
  - https://quarkus.io/guides/config-reference
  - https://quarkus.io/guides/config-mappings
---

## O que é

O jeito do Quarkus de ler o `application.properties`. Uma propriedade sozinha vem com
`@ConfigProperty(name = "loja.frete.base")`, num campo ou parâmetro. Um grupo de propriedades com
o mesmo prefixo vira uma interface com `@ConfigMapping(prefix = "loja.frete")`: cada método é uma
propriedade, e o Quarkus gera a implementação.

As propriedades são checadas na inicialização: se uma obrigatória falta ou tem o tipo errado, a
aplicação nem sobe, em vez de quebrar no meio de uma requisição.

## Exemplo

```java
@ConfigMapping(prefix = "loja.frete")
interface FreteConfig {

    BigDecimal base();

    BigDecimal porQuilo();
}
```

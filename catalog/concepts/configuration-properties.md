---
id: configuration-properties
nome: Configuração tipada (@ConfigurationProperties)
grupo: spring
ordem: 80
aparece_em:
  - anotacao: ConfigurationProperties
referencias:
  - https://docs.spring.io/spring-boot/reference/features/external-config.html#features.external-config.typesafe-configuration-properties
---

## O que é

Uma classe (de preferência um record) que recebe um grupo de propriedades do
`application.properties` ou `application.yml`, todas com o mesmo prefixo. Em vez de várias
strings soltas com `@Value("${...}")`, a configuração vira um tipo: convertida, validável e
declarada num lugar só.

Valores como `30s` viram `Duration` e `10MB` vira `DataSize` sozinhos, e a IDE autocompleta as
chaves no `application.yml`.

## Exemplo

```java
@ConfigurationProperties(prefix = "loja.frete")
record FreteProperties(BigDecimal valorPorQuilo, Duration prazoMaximo) {
}
```

---
id: value-outside-constructor
referencias:
  - https://docs.spring.io/spring-boot/reference/features/external-config.html#features.external-config.typesafe-configuration-properties.vs-value-annotation
  - https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/value-annotations.html
---

## Por quê

`@Value` em campo tem os problemas da injeção em campo: o valor chega por reflexão depois que
o objeto existe, o campo não pode ser `final` e, num teste sem Spring, ele fica `null` (ou `0`)
em silêncio. Como parâmetro do construtor, a configuração entra no que a classe declara
precisar, e o teste passa o valor direto.

Quando são várias propriedades do mesmo grupo, `@ConfigurationProperties` vai além: converte os
tipos, aceita as variações de nome (`item-price`, `itemPrice`, `ITEMPRICE`) e gera os metadados
que a IDE usa para autocompletar o `application.yml`. A documentação do Spring Boot recomenda
agrupar assim as chaves dos seus próprios componentes.

## Quando é aceitável

`@Value` continua sendo a ferramenta para expressões SpEL (`#{...}`), que
`@ConfigurationProperties` não avalia. Num método `@Bean` de uma classe `@Configuration`, um
`@Value` no parâmetro é uma forma legítima de passar configuração; a kata acusa porque não está
num construtor, então avalie caso a caso.

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

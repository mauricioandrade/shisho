---
id: configuration-properties-not-record
referencias:
  - https://docs.spring.io/spring-boot/reference/features/external-config.html#features.external-config.typesafe-configuration-properties.constructor-binding
---

## Por quê

Configuração é lida uma vez, na inicialização, e não deveria mudar depois. Com setters,
qualquer bean que recebe o objeto pode alterá-lo, e a mudança afeta todos os outros que o
compartilham. Como record, a ligação é feita pelo construtor (constructor binding), os
componentes são `final` e o objeto é de fato imutável.

O record também deixa claro o que existe: valores padrão vêm de `@DefaultValue` no componente,
e, com `@Validated`, um valor inválido derruba a inicialização em vez de aparecer na primeira
vez que é usado.

## Quando é aceitável

Uma classe com setters continua funcionando; o ganho é imutabilidade, não correção. Ela ainda
é necessária quando as propriedades são ligadas a um objeto criado num método `@Bean`, porque o
constructor binding só funciona com `@EnableConfigurationProperties` ou
`@ConfigurationPropertiesScan`.

No Spring Boot 2.2 a 2.5, o record ainda precisa de `@ConstructorBinding` na classe; a partir do
2.6, um record com um construtor só dispensa a anotação. Abaixo do Java 16 não há record: a kata
vira sugestão para depois da migração.

## Antes

```java
@ConfigurationProperties(prefix = "loja.email")
public class EmailProperties {

    private String host;
    private int porta;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPorta() {
        return porta;
    }

    public void setPorta(int porta) {
        this.porta = porta;
    }
}
```

## Depois

Registre o record com `@EnableConfigurationProperties(EmailProperties.class)` ou
`@ConfigurationPropertiesScan`. Validações como `@NotBlank` funcionam nos componentes, com
`@Validated` no record.

```java
@ConfigurationProperties(prefix = "loja.email")
public record EmailProperties(String host, int porta) {
}
```

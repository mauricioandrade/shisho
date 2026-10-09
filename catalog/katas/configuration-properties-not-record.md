---
id: configuration-properties-not-record
---

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

---
id: prefer-configuration-properties
---

## Antes

```java
@Service
public class EmailService {

    public EmailService(
            @Value("${loja.email.host}") String host,
            @Value("${loja.email.porta}") int porta,
            @Value("${loja.email.remetente}") String remetente) {
        this.cliente = new ClienteSmtp(host, porta, remetente);
    }
}
```

## Depois

```java
@ConfigurationProperties(prefix = "loja.email")
public record EmailProperties(String host, int porta, String remetente) {
}

@Service
public class EmailService {

    public EmailService(EmailProperties email) {
        this.cliente = new ClienteSmtp(email.host(), email.porta(), email.remetente());
    }
}
```

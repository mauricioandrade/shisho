---
id: prefer-configuration-properties
---

## Why

Several properties with the same prefix are one single concept (the email settings, an HTTP
client's) scattered across loose strings. A typo in `${shop.email.host}` only shows up at
runtime, and every class that needs the settings repeats the keys.

With `@ConfigurationProperties`, the group becomes a type: the keys are declared once, the values
are converted (`Duration`, `DataSize`, lists) and validated at startup, and with
`spring-boot-configuration-processor` the IDE autocompletes `application.yml`. The Spring Boot
documentation recommends this route for your own components' keys.

In Quarkus, the equivalent of `@Value` is MicroProfile Config's `@ConfigProperty`, and the
equivalent of `@ConfigurationProperties` is `@ConfigMapping(prefix = "mail")`, on an interface:
Quarkus generates the implementation, validates the properties at startup and fails early if one
is missing.

## Acceptable when

With two or three properties used in a single place, `@Value` in the constructor is simple and
enough (the kata only shows up from three with the same prefix). And `@Value` is the only option
for SpEL expressions.

## Before

```java
@Service
public class EmailService {

    public EmailService(
            @Value("${shop.email.host}") String host,
            @Value("${shop.email.port}") int port,
            @Value("${shop.email.sender}") String sender) {
        this.client = new SmtpClient(host, port, sender);
    }
}
```

## After

```java
@ConfigurationProperties(prefix = "shop.email")
public record EmailProperties(String host, int port, String sender) {
}

@Service
public class EmailService {

    public EmailService(EmailProperties email) {
        this.client = new SmtpClient(email.host(), email.port(), email.sender());
    }
}
```

## Before (Quarkus)

```java
@ApplicationScoped
public class EmailService {

    EmailService(
            @ConfigProperty(name = "shop.email.host") String host,
            @ConfigProperty(name = "shop.email.port") int port,
            @ConfigProperty(name = "shop.email.sender") String sender) {
        this.client = new SmtpClient(host, port, sender);
    }
}
```

## After (Quarkus)

Each method of the interface is the property with its name (`port()` reads `shop.email.port`),
and Quarkus generates the implementation. The class takes the interface in its constructor, like
any bean.

```java
@ConfigMapping(prefix = "shop.email")
public interface EmailConfig {

    String host();

    int port();

    String sender();
}

@ApplicationScoped
public class EmailService {

    EmailService(EmailConfig email) {
        this.client = new SmtpClient(email.host(), email.port(), email.sender());
    }
}
```

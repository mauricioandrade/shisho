---
id: configuration-properties-not-record
---

## Why

Configuration is read once, at startup, and should not change afterwards. With setters, any bean
that receives the object can change it, and the change affects every other bean sharing it. As
a record, binding happens through the constructor (constructor binding), the components are
`final` and the object really is immutable.

The record also makes clear what exists: defaults come from `@DefaultValue` on the component,
and with `@Validated` an invalid value stops the startup instead of surfacing the first time it
is used.

## Acceptable when

A class with setters still works; the gain is immutability, not correctness. It is still needed
when the properties are bound to an object created in a `@Bean` method, because constructor
binding only works with `@EnableConfigurationProperties` or `@ConfigurationPropertiesScan`.

In Spring Boot 2.2 to 2.5, the record still needs `@ConstructorBinding` on the class; from 2.6
on, a record with a single constructor does without it. Below Java 16 there are no records: the
kata becomes a suggestion for after the upgrade.

## Before

```java
@ConfigurationProperties(prefix = "shop.email")
public class EmailProperties {

    private String host;
    private int port;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
```

## After

Register the record with `@EnableConfigurationProperties(EmailProperties.class)` or
`@ConfigurationPropertiesScan`. Validations such as `@NotBlank` work on the components, with
`@Validated` on the record.

```java
@ConfigurationProperties(prefix = "shop.email")
public record EmailProperties(String host, int port) {
}
```

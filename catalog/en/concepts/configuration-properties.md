---
id: configuration-properties
nome: Typed configuration (@ConfigurationProperties)
---

## What it is

A class (preferably a record) that receives a group of properties from `application.properties`
or `application.yml`, all sharing the same prefix. Instead of several loose strings with
`@Value("${...}")`, the configuration becomes a type: converted, validatable and declared in one
place.

Values such as `30s` become a `Duration` and `10MB` a `DataSize` on their own, and the IDE
autocompletes the keys in `application.yml`.

## Example

```java
@ConfigurationProperties(prefix = "shop.shipping")
record ShippingProperties(BigDecimal pricePerKilo, Duration maximumDelay) {
}
```

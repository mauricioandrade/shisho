---
id: config-mapping
nome: Configuration in Quarkus (@ConfigMapping)
---

## What it is

The Quarkus way of reading `application.properties`. A single property comes with
`@ConfigProperty(name = "shop.shipping.base")`, on a field or parameter. A group of properties with
the same prefix becomes an interface with `@ConfigMapping(prefix = "shop.shipping")`: each method
is a property, and Quarkus generates the implementation.

Properties are checked at startup: if a required one is missing or has the wrong type, the
application does not even start, instead of breaking in the middle of a request.

## Example

```java
@ConfigMapping(prefix = "shop.shipping")
interface ShippingConfig {

    BigDecimal base();

    BigDecimal perKilo();
}
```

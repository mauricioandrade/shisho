---
id: value-outside-constructor
---

## Why

`@Value` on a field has the problems of field injection: the value arrives by reflection after
the object exists, the field cannot be `final` and, in a test without Spring, it silently stays
`null` (or `0`). As a constructor parameter, the setting becomes part of what the class declares
it needs, and the test passes the value directly.

When there are several properties from the same group, `@ConfigurationProperties` goes further:
it converts types, accepts name variations (`item-price`, `itemPrice`, `ITEMPRICE`) and generates
the metadata the IDE uses to autocomplete `application.yml`. The Spring Boot documentation
recommends grouping your own components' keys that way.

## Acceptable when

`@Value` is still the tool for SpEL expressions (`#{...}`), which `@ConfigurationProperties` does
not evaluate. In a `@Bean` method of a `@Configuration` class, `@Value` on the parameter is a
legitimate way to pass configuration; the kata flags it because it is not in a constructor, so
judge case by case.

## Before

```java
@Service
public class OrderService {

    @Value("${shop.max-items}")
    private int maxItems;
}
```

## After

```java
@Service
public class OrderService {

    private final int maxItems;

    public OrderService(@Value("${shop.max-items}") int maxItems) {
        this.maxItems = maxItems;
    }
}
```

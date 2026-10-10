---
id: flexible-constructor-bodies
nome: Flexible constructor bodies
---

## What it is

Code before `super(...)` or `this(...)`: validating arguments and preparing values with no static
helper method.

## Example

```java
Order(String customer) {
    if (customer.isBlank()) {
        throw new IllegalArgumentException("customer is required");
    }
    super();
    this.customer = customer;
}
```

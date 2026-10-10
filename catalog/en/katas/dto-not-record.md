---
id: dto-not-record
---

## Why

A DTO only carries data from one side to the other, and a record says so in its very
declaration: the components are `final`, and the compiler generates the constructor, accessors,
`equals`, `hashCode` and `toString` (JEP 395). With no hand-written getters and setters, there is
nowhere to forget a field in `equals`, and an immutable DTO does not change on its way from the
controller to the service.

The ecosystem keeps up: Jackson reads and writes records since version 2.12, and Bean Validation
accepts annotations on the components.

## Acceptable when

When a framework requires a no-argument constructor and setters, such as old mapping libraries,
or when the DTO has to extend another class: a record has no class inheritance (but it can
implement interfaces, `sealed` ones included). JPA entities cannot be records either, but an
entity is not a DTO.

## Before

```java
public class CreateOrderRequest {

    private String customer;
    private int quantity;

    public String getCustomer() {
        return customer;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
```

## After

Jackson and Bean Validation work with records: the annotations go on the components.

```java
public record CreateOrderRequest(@NotBlank String customer, @Positive int quantity) {
}
```

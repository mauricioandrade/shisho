---
id: entity-in-controller
---

## Why

Returning the entity turns the database schema into the API's public contract: renaming a column
or adding an internal field breaks the clients, and fields that should stay inside (a password
hash, audit flags) end up in the JSON. Receiving the entity as `@RequestBody` turns the problem
around: the client can fill in fields it should never touch, such as `id`, `role` or `balance`.
OWASP groups both cases (excessive data exposure and mass assignment) under API3:2023.

Then there is the JPA side. Serializing an entity with `LAZY` relationships fires queries while
the JSON is being written (or a `LazyInitializationException`), and bidirectional relationships
recurse forever. Spring Boot turns Open EntityManager in View on by default precisely so this
lazy loading works, at the cost of holding the database connection for the whole request.

## Acceptable when

In prototypes and throwaway internal tools, where the API and the database change together and
the client is yourself. Even then, avoid receiving the entity as `@RequestBody`. Spring Data
projections (interfaces or records) are a cheap way to get a DTO without mapping by hand.

## Before

```java
@Entity
class Order {
    @Id private Long id;
    private String customer;
    private String internalNote;
}

@RestController
class OrderController {

    @GetMapping("/orders/{id}")
    Order find(@PathVariable Long id) {
        return orders.find(id);
    }
}
```

## After

The response record chooses what goes out through the API. Renaming a column or adding an
internal field to the entity no longer changes the contract.

```java
@Entity
class Order {
    @Id private Long id;
    private String customer;
    private String internalNote;
}

record OrderResponse(Long id, String customer) {

    static OrderResponse of(Order order) {
        return new OrderResponse(order.getId(), order.getCustomer());
    }
}

@RestController
class OrderController {

    @GetMapping("/orders/{id}")
    OrderResponse find(@PathVariable Long id) {
        return OrderResponse.of(orders.find(id));
    }
}
```

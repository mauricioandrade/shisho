---
id: dto
nome: DTO (Data Transfer Object)
---

## What it is

An object that only carries data from one side to the other, with no business rules: what the
client sends (`Request`) and what the API returns (`Response`). It separates the API contract
from the internal model: the entity can change without breaking API consumers, and internal
fields do not leak.

In modern Java, a DTO is almost always a record: immutable and free of boilerplate.

## Example

```java
record OrderResponse(Long id, String customer, BigDecimal total) {

    static OrderResponse of(Order order) {
        return new OrderResponse(order.getId(), order.getCustomer(), order.total());
    }
}
```

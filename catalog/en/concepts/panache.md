---
id: panache
nome: Hibernate ORM with Panache
---

## What it is

A Quarkus layer over Hibernate that removes the repetitive data access code, in two ways. In
Active Record, the entity extends `PanacheEntity` and already knows how to save and find itself:
`order.persist()`, `Order.findById(id)`. In Repository, a class implements
`PanacheRepository<Order>` and gets the same methods, while the entity keeps only the data.

`PanacheEntity` also brings a ready `id`, and queries accept a piece of JPQL:
`Order.list("customer", name)`. Both ways are legitimate; Active Record is shorter, and Repository
keeps database rules better apart.

## Example

```java
@Entity
public class Order extends PanacheEntity {

    public String customer;

    public static List<Order> ofCustomer(String customer) {
        return list("customer", customer);
    }
}
```

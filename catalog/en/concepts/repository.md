---
id: repository
nome: Repository
---

## What it is

An object that looks like an in-memory collection of entities (`findById`, `save`, `delete`) and
hides how they are stored. Whoever uses the repository writes no SQL and does not know whether a
relational database, a document store or a service sits on the other side.

With Spring Data, you only declare the interface extending `JpaRepository`, and the
implementation is generated. Even the queries: a method `findByCustomer(String customer)` becomes
SQL from its name.

## Example

```java
interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomer(String customer);
}
```

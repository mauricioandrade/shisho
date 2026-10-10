---
id: transactional-read-only
---

## Why

`readOnly = true` tells the transaction manager that nothing will be written. With
JPA/Hibernate the effect is concrete: the session stops flushing automatically and, since
Spring 5.1, the loaded entities are in read-only mode, so Hibernate drops the copy it would keep
for dirty checking. That is less memory and less work at the end of the transaction, and the
difference grows with the number of entities read.

The annotation also documents the intent: whoever reads it knows the method should not change
anything.

## Acceptable when

It is a hint, not a lock: the javadoc warns that writes do not necessarily fail. If the method
reads in order to write (loads the entity and changes a field), it is not a read method, even if
its name starts with `find`; the kata decides by name and can be wrong. Spring Data repository
methods are already `readOnly` by default, so the gain shows up in the service that groups
several reads in one transaction.

## Before

```java
@Service
public class OrderService {

    @Transactional
    public List<Order> findByCustomer(String customer) {
        return orders.findByCustomer(customer);
    }
}
```

## After

A common pattern is marking the whole class as read-only and only the methods that write with
`@Transactional`.

```java
@Service
@Transactional(readOnly = true)
public class OrderService {

    public List<Order> findByCustomer(String customer) {
        return orders.findByCustomer(customer);
    }

    @Transactional
    public Order create(Order order) {
        return orders.save(order);
    }
}
```

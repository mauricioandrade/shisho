---
id: eager-collection
---

## Why

`fetch = FetchType.EAGER` on a collection tells Hibernate: whenever you load this entity, load the
collection with it. "Whenever" includes the screen that only shows the customer's name, the report
that counts orders and the endpoint that returns a list of a hundred customers. On a list, Hibernate
runs one query for the customers and one more for each customer's orders (the famous N+1), or joins
everything in a JOIN that multiplies the rows.

The problem is that the decision sits in the wrong place. Whether the collection is needed depends
on the use case, and the mapping is one for all of them. Leave the collection `LAZY`, already the
default for `@OneToMany` and `@ManyToMany`, and fetch it only where it is used: with `JOIN FETCH` in
the query or with `@EntityGraph` on the repository.

## Acceptable when

On a small collection always used together with the entity, such as the items of an order that is
never shown without them. Even then, an `@EntityGraph` on the repository method makes the decision
explicit, and the entity does not force its other uses to load what they do not need.

## Before

```java
@Entity
public class Customer {

    @Id
    @GeneratedValue
    private Long id;

    @OneToMany(mappedBy = "customer", fetch = FetchType.EAGER)
    private List<Order> orders = new ArrayList<>();
}
```

## After

```java
@Entity
public class Customer {

    @Id
    @GeneratedValue
    private Long id;

    @OneToMany(mappedBy = "customer")
    private List<Order> orders = new ArrayList<>();
}
```

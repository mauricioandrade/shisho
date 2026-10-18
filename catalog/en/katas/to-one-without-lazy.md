---
id: to-one-without-lazy
---

## Why

In JPA, "to-many" relationships are `LAZY` by default, and "to-one" ones (`@ManyToOne`,
`@OneToOne`) are `EAGER`. It is an asymmetry that catches almost everyone: a `@ManyToOne` without
`fetch` loads the other side on every query. A list of a hundred orders, each with its customer,
becomes one query for the orders and up to a hundred for the customers, the N+1 nobody wrote on
purpose.

With `fetch = FetchType.LAZY`, Hibernate puts a proxy in its place that only goes to the database
if someone uses the customer. Where the customer is used, a `JOIN FETCH` brings everything in a
single query.

## Acceptable when

When the other side is used practically always and `EAGER` is a choice: write
`fetch = FetchType.EAGER` and the kata accepts it, because the decision became visible. The inverse
side of a `@OneToOne(mappedBy = ...)` is left out too: without bytecode enhancement Hibernate cannot
make it lazy, so `fetch` there changes nothing.

## Before

```java
@Entity
public class Order {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private Customer customer;
}
```

## After

```java
@Entity
public class Order {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Customer customer;
}
```

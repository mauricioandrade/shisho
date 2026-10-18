---
id: entity-with-lombok-data
---

## Why

`@Data` is great for data classes and dangerous on a JPA entity, because the `equals`, `hashCode`
and `toString` it generates use **every** field. On an entity that runs into three things
Hibernate does underneath:

- **The id changes.** A new entity has a null `id`, and `persist` fills it in. With `hashCode`
  computed from it, the same entity gets another hash: stored in a `HashSet` before saving, it can
  no longer be found in there.
- **Lazy collections.** `hashCode` and `toString` walk the relationships. Outside the transaction
  that is a `LazyInitializationException`; inside, it fires queries nobody asked for.
- **Two-way references.** `Order` has items, and each `Item` points back to the `Order`. One's
  `toString` calls the other's, until the stack overflows.

The safe way is `@Getter` and `@Setter`, and a hand-written `equals` by the id, with a constant
`hashCode` for the class, which does not change when the id appears.

## Acceptable when

When `equals` and `hashCode` are chosen on purpose: `@EqualsAndHashCode(of = "code")`, by a
natural key that exists before `persist`, or `onlyExplicitlyIncluded = true` with the fields
marked. The kata only flags `@Data` and a bare `@EqualsAndHashCode`, which take every field.

## Before

```java
@Entity
@Data
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue
    private Long id;

    @OneToMany(mappedBy = "order")
    private List<Item> items = new ArrayList<>();
}
```

## After

```java
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue
    private Long id;

    @OneToMany(mappedBy = "order")
    private List<Item> items = new ArrayList<>();

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof Order order && id != null && id.equals(order.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
```

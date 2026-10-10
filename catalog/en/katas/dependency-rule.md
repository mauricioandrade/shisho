---
id: dependency-rule
---

## Why

The dependency rule is what gives architectures such as hexagonal and clean their meaning: the
domain does not know the details (database, web, frameworks), and dependencies point inward. That
way business rules are tested without infrastructure, and infrastructure changes without touching
the core.

A violation breaks nothing today; the cost shows up slowly. Every import pointing outward is a
coupling that makes the package split decorative: the structure promises an isolation the code
does not keep.

## Acceptable when

If the architecture brings the project no benefit (a simple CRUD organized as hexagonal), the way
out may be simplifying the architecture rather than creating ports and adapters just to respect
the rule: see the overengineering verdict. An isolated violation, made consciously and
documented, can also be an acceptable price.

## Before

The domain knows a persistence detail: replacing JPA with something else forces changes in the
core.

```java
package com.shop.domain;

import com.shop.infrastructure.persistence.OrderJpaRepository;

public class OrderClosing {

    private final OrderJpaRepository orders;
}
```

## After

The domain declares what it needs (a port); the infrastructure implements it. The dependency
points inward.

```java
package com.shop.domain;

public interface Orders {

    Order find(OrderId id);
}

public class OrderClosing {

    private final Orders orders;
}
```

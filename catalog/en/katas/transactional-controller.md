---
id: transactional-controller
---

## Why

A transaction exists to protect a business rule: "debit and credit, or neither". The place for
that rule is the service, and that is where the transaction boundary makes sense. When
`@Transactional` moves to the controller, two things happen.

The first is about design: if the transaction is in the controller, so is the rule it protects, and
the controller stops only translating HTTP. The second is practical: the transaction (and the
database connection it holds) lasts the whole handling of the request, from reading the body to
building the response, and not only the database work.

## Acceptable when

In a CRUD without a service layer, where the controller calls the repository directly and there is
no rule to protect. It is what JHipster generates when you choose not to have services. If the
application grows, the first step is creating the service and taking the transaction there. The
kata does not run on Quarkus: there, a resource with `@Transactional` is the way the Quarkus guides
themselves teach.

## Before

```java
@RestController
@RequestMapping("/transfers")
public class TransferController {

    @PostMapping
    @Transactional
    public void transfer(@Valid @RequestBody TransferRequest request) {
        accounts.debit(request.from(), request.amount());
        accounts.credit(request.to(), request.amount());
    }
}
```

## After

```java
@RestController
@RequestMapping("/transfers")
public class TransferController {

    @PostMapping
    public void transfer(@Valid @RequestBody TransferRequest request) {
        transfers.transfer(request.from(), request.to(), request.amount());
    }
}
```

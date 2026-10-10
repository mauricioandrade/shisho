---
id: controller-rest
nome: REST controller
---

## What it is

The application's HTTP entry point. Each method annotated with `@GetMapping`, `@PostMapping` and
the like serves a route, and Spring turns the request's JSON into Java objects and the method's
return value back into JSON.

A good controller only translates: it reads the request, calls the service and picks the response
status. Business rules live in the service, where they can be tested without HTTP.

## Example

```java
@RestController
@RequestMapping("/orders")
class OrderController {

    private final OrderService service;

    OrderController(OrderService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    OrderResponse find(@PathVariable Long id) {
        return service.find(id);
    }
}
```

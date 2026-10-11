---
id: jakarta-rest
nome: REST resources (Jakarta REST)
---

## What it is

The application's HTTP door, in the Jakarta REST standard (formerly JAX-RS): what Spring calls a
controller, Quarkus calls a resource. `@Path` states the URL, and `@GET`, `@POST`, `@PUT` and
`@DELETE` state which HTTP method each Java method serves.

The parameters state where each piece of data comes from: `{id}` in `@Path` becomes the `id`
parameter, `@RestQuery` reads the query string, and the parameter with no annotation is the body,
converted from JSON. The return value becomes the response JSON.

## Example

```java
@Path("/orders")
public class OrderResource {

    @Inject
    OrderService service;

    @GET
    @Path("{id}")
    public OrderResponse find(Long id) {
        return service.find(id);
    }
}
```

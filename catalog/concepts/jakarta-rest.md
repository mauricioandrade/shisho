---
id: jakarta-rest
nome: Resources REST (Jakarta REST)
grupo: quarkus
ordem: 20
aparece_em:
  - anotacao: Path
referencias:
  - https://quarkus.io/guides/rest
  - https://jakarta.ee/specifications/restful-ws/
---

## O que é

A porta HTTP da aplicação, no padrão Jakarta REST (o antigo JAX-RS): o que o Spring chama de
controller, o Quarkus chama de resource. `@Path` diz a URL, e `@GET`, `@POST`, `@PUT` e
`@DELETE` dizem que método HTTP cada método Java atende.

Os parâmetros dizem de onde vem cada dado: `{id}` no `@Path` vira o parâmetro `id`, `@RestQuery`
lê a query string, e o parâmetro sem anotação é o corpo, convertido de JSON. O retorno vira o JSON
da resposta.

## Exemplo

```java
@Path("/pedidos")
public class PedidoResource {

    @Inject
    PedidoService service;

    @GET
    @Path("{id}")
    public PedidoResponse buscar(Long id) {
        return service.buscar(id);
    }
}
```

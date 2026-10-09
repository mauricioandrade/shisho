---
id: entity-in-controller
referencias:
  - https://api-security.owasp.org/editions/2023/en/0xa3-broken-object-property-level-authorization
  - https://docs.spring.io/spring-boot/reference/data/sql.html#data.sql.jpa-and-spring-data.open-entity-manager-in-view
---

## Por quê

Ao devolver a entidade, o esquema do banco vira o contrato público da API: renomear uma coluna
ou adicionar um campo interno quebra os clientes, e campos que deviam ficar dentro (hash de
senha, flags de auditoria) saem no JSON. Ao receber a entidade como `@RequestBody`, o problema
se inverte: o cliente pode preencher campos que nunca deveria tocar, como `id`, `perfil` ou
`saldo`. A OWASP agrupa os dois casos (exposição excessiva e mass assignment) no API3:2023.

Há ainda o lado do JPA. Serializar uma entidade com relacionamentos `LAZY` dispara consultas
durante a escrita do JSON (ou `LazyInitializationException`), e relacionamentos bidirecionais
entram em recursão. O Spring Boot liga o Open EntityManager in View por padrão justamente para
esse lazy loading funcionar, ao custo de segurar a conexão com o banco durante a requisição
inteira.

## Quando é aceitável

Em protótipos e ferramentas internas descartáveis, em que a API e o banco mudam juntos e o
cliente é você mesmo. Mesmo aí, evite receber a entidade como `@RequestBody`. As projeções do
Spring Data (interfaces ou records) são um jeito barato de ter um DTO sem mapear à mão.

## Antes

```java
@Entity
class Pedido {
    @Id private Long id;
    private String cliente;
    private String observacaoInterna;
}

@RestController
class PedidoController {

    @GetMapping("/pedidos/{id}")
    Pedido buscar(@PathVariable Long id) {
        return pedidos.buscar(id);
    }
}
```

## Depois

O record de resposta escolhe o que sai na API. Renomear uma coluna ou adicionar um campo
interno à entidade não muda mais o contrato.

```java
@Entity
class Pedido {
    @Id private Long id;
    private String cliente;
    private String observacaoInterna;
}

record PedidoResponse(Long id, String cliente) {

    static PedidoResponse de(Pedido pedido) {
        return new PedidoResponse(pedido.getId(), pedido.getCliente());
    }
}

@RestController
class PedidoController {

    @GetMapping("/pedidos/{id}")
    PedidoResponse buscar(@PathVariable Long id) {
        return PedidoResponse.de(pedidos.buscar(id));
    }
}
```

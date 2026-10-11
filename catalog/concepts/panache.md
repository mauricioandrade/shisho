---
id: panache
nome: Hibernate ORM com Panache
grupo: quarkus
ordem: 30
aparece_em:
  - estende: PanacheEntity
  - estende: PanacheEntityBase
  - estende: PanacheRepository
  - estende: PanacheRepositoryBase
referencias:
  - https://quarkus.io/guides/hibernate-orm-panache
---

## O que é

Uma camada do Quarkus sobre o Hibernate que tira o código repetitivo do acesso a dados, de dois
jeitos. No Active Record, a entidade estende `PanacheEntity` e já sabe se salvar e se buscar:
`pedido.persist()`, `Pedido.findById(id)`. No Repository, uma classe implementa
`PanacheRepository<Pedido>` e ganha os mesmos métodos, com a entidade ficando só com os dados.

O `PanacheEntity` também traz o `id` pronto, e as consultas aceitam um pedaço de JPQL:
`Pedido.list("cliente", nome)`. Os dois jeitos são legítimos; o Active Record é mais curto, e o
Repository separa melhor a regra do banco.

## Exemplo

```java
@Entity
public class Pedido extends PanacheEntity {

    public String cliente;

    public static List<Pedido> doCliente(String cliente) {
        return list("cliente", cliente);
    }
}
```

---
id: repository
nome: Repository
grupo: padrao
ordem: 10
aparece_em:
  - estende: JpaRepository
  - estende: CrudRepository
  - estende: ListCrudRepository
  - estende: PagingAndSortingRepository
  - tipo: interface
    sufixo: Repository
referencias:
  - https://martinfowler.com/eaaCatalog/repository.html
  - https://docs.spring.io/spring-data/jpa/reference/repositories/core-concepts.html
---

## O que é

Um objeto que parece uma coleção de entidades em memória (`findById`, `save`, `delete`) e esconde
como elas são guardadas. Quem usa o repositório não escreve SQL nem sabe se há um banco
relacional, um documento ou um serviço do outro lado.

Com o Spring Data, você só declara a interface estendendo `JpaRepository`, e a implementação é
gerada. Até as consultas: um método `findByCliente(String cliente)` vira SQL pelo nome.

## Exemplo

```java
interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByCliente(String cliente);
}
```

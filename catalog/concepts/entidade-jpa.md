---
id: entidade-jpa
nome: Entidade JPA
grupo: jakarta
ordem: 10
aparece_em:
  - anotacao: Entity
referencias:
  - https://jakarta.ee/specifications/persistence/3.2/
  - https://docs.spring.io/spring-data/jpa/reference/jpa/entity-persistence.html
---

## O que é

Uma classe que representa uma tabela do banco: cada objeto é uma linha, cada campo uma coluna.
O `@Id` marca a chave primária, e o JPA (com o Hibernate por baixo) gera o SQL para buscar,
inserir e atualizar.

Dentro de uma transação, a entidade é gerenciada: o Hibernate percebe o que mudou nela e
grava sozinho no fim, sem você chamar `save`. Por isso ela não deve ir direto para a API: o
contrato do JSON fica preso ao esquema do banco.

## Exemplo

```java
@Entity
class Pedido {

    @Id
    @GeneratedValue
    private Long id;
    private String cliente;

    protected Pedido() {
    }
}
```

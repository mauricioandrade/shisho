---
id: active-record
nome: Active Record
grupo: padrao
ordem: 15
aparece_em:
  - estende: PanacheEntity
  - estende: PanacheEntityBase
referencias:
  - https://martinfowler.com/eaaCatalog/activeRecord.html
  - https://quarkus.io/guides/hibernate-orm-panache#solution-1-using-the-active-record-pattern
---

## O que é

Um objeto que é ao mesmo tempo a linha da tabela e o código que a grava: `pedido.persist()`,
`Pedido.findById(id)`. Não há um repositório separado; a própria classe sabe se salvar e se
buscar.

É o oposto do Repository. Fica mais curto e direto quando a entidade é quase só dados, como num
CRUD. Quando o domínio ganha regras, misturar regra e banco na mesma classe começa a pesar, e
separar um repositório volta a valer a pena.

## Exemplo

```java
@Entity
public class Pedido extends PanacheEntity {

    public String cliente;

    public void cancelar() {
        delete();
    }
}
```

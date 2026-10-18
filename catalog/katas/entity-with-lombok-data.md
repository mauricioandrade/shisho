---
id: entity-with-lombok-data
referencias:
  - https://vladmihalcea.com/the-best-way-to-implement-equals-hashcode-and-tostring-with-jpa-and-hibernate/
  - https://thorben-janssen.com/lombok-hibernate-how-to-avoid-common-pitfalls/
---

## Por quê

O `@Data` é ótimo para classes de dados e perigoso numa entidade JPA, porque o `equals`, o
`hashCode` e o `toString` que ele gera usam **todos** os campos. Numa entidade, isso esbarra em
três coisas que o Hibernate faz por baixo:

- **O id muda.** Uma entidade nova tem `id` nulo, e o `persist` preenche o id. Com o `hashCode`
  calculado a partir dele, a mesma entidade passa a ter outro hash: guardada num `HashSet` antes de
  salvar, ela não é mais encontrada lá dentro.
- **Coleções lazy.** O `hashCode` e o `toString` percorrem os relacionamentos. Fora da transação,
  isso dá `LazyInitializationException`; dentro, dispara consultas que ninguém pediu.
- **Referências de mão dupla.** `Pedido` tem itens, e cada `Item` aponta para o `Pedido`. O
  `toString` de um chama o do outro, até estourar a pilha.

O caminho seguro é `@Getter` e `@Setter`, e um `equals` escrito à mão pelo id, com um `hashCode`
constante para a classe, que não muda quando o id aparece.

## Quando é aceitável

Quando o `equals` e o `hashCode` são escolhidos de propósito: `@EqualsAndHashCode(of = "codigo")`,
por uma chave natural que existe antes do `persist`, ou `onlyExplicitlyIncluded = true` com os
campos marcados. A kata só acusa o `@Data` e o `@EqualsAndHashCode` sem atributos, que pegam todos
os campos.

## Antes

```java
@Entity
@Data
@NoArgsConstructor
public class Pedido {

    @Id
    @GeneratedValue
    private Long id;

    @OneToMany(mappedBy = "pedido")
    private List<Item> itens = new ArrayList<>();
}
```

## Depois

```java
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Pedido {

    @Id
    @GeneratedValue
    private Long id;

    @OneToMany(mappedBy = "pedido")
    private List<Item> itens = new ArrayList<>();

    @Override
    public boolean equals(Object outro) {
        return this == outro || outro instanceof Pedido pedido && id != null && id.equals(pedido.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
```

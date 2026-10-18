---
id: dto-not-record
referencias:
  - https://openjdk.org/jeps/395
  - https://github.com/FasterXML/jackson-databind/issues/2709
---

## Por quê

Um DTO só carrega dados de um lado para outro, e um record diz isso na própria declaração: os
componentes são `final`, e o compilador gera construtor, acessores, `equals`, `hashCode` e
`toString` (JEP 395). Sem getters e setters escritos à mão, não há onde esquecer um campo no
`equals`, e um DTO imutável não muda no caminho entre o controller e o service.

O ecossistema acompanha: o Jackson lê e escreve records desde a versão 2.12, e o Bean
Validation aceita as anotações nos componentes.

## Quando é aceitável

Quando um framework exige construtor sem argumentos e setters, como bibliotecas antigas de
mapeamento, ou quando o DTO precisa estender outra classe: um record não tem herança de classe
(mas pode implementar interfaces, inclusive `sealed`). Entidades JPA também não podem ser
records, mas entidade não é DTO.

Um builder (`PetDetailsBuilder`) a kata não acusa: ele é mutável por definição. Num pacote `dto`,
também não acusa uma classe sem estado, como um mapper com métodos estáticos.

## Antes

```java
public class CriarPedidoRequest {

    private String cliente;
    private int quantidade;

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }
}
```

## Depois

O Jackson e o Bean Validation funcionam com records: as anotações vão nos componentes.

```java
public record CriarPedidoRequest(@NotBlank String cliente, @Positive int quantidade) {
}
```

---
id: entity-without-default-constructor
referencias:
  - https://jakarta.ee/specifications/persistence/3.1/jakarta-persistence-spec-3.1
  - https://docs.jboss.org/hibernate/orm/6.6/userguide/html_single/Hibernate_User_Guide.html
---

## Por quê

Quando o Hibernate lê uma linha do banco, ele não sabe chamar o construtor que você escreveu: não
sabe que argumento vai em qual parâmetro. Ele cria a entidade com o construtor **sem argumentos** e
preenche os campos depois, por reflexão. A especificação do JPA exige esse construtor, `public` ou
`protected`.

Uma classe sem construtor nenhum tem o padrão, que o Java gera. O problema aparece quando alguém
escreve um construtor com parâmetros, para criar a entidade já válida, e o padrão some sem aviso. O
código compila e sobe, e a primeira consulta falha com `InstantiationException`.

## Quando é aceitável

Nunca numa entidade. A saída é um construtor `protected` sem argumentos ao lado do seu: o JPA
aceita, e o resto do código, que não está no mesmo pacote nem é subclasse, continua obrigado a usar
o construtor com parâmetros. O `@NoArgsConstructor(access = AccessLevel.PROTECTED)` do Lombok faz o
mesmo, e a kata o reconhece. Um record mapeado como `@Embeddable` fica de fora: o Hibernate 6.2 usa
o construtor canônico dele.

## Antes

```java
@Entity
public class Produto {

    @Id
    @GeneratedValue
    private Long id;

    private String nome;

    public Produto(String nome) {
        this.nome = nome;
    }
}
```

## Depois

```java
@Entity
public class Produto {

    @Id
    @GeneratedValue
    private Long id;

    private String nome;

    protected Produto() {
    }

    public Produto(String nome) {
        this.nome = nome;
    }
}
```

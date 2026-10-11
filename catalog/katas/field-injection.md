---
id: field-injection
referencias:
  - https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html
  - https://odrotbohm.de/2013/11/why-field-injection-is-evil/
  - https://docs.spring.io/spring-boot/reference/using/spring-beans-and-dependency-injection.html
  - https://quarkus.io/guides/cdi-reference#native-executables-and-private-members
---

## Por quê

A equipe do Spring recomenda injeção por construtor: o objeto já nasce completo, as
dependências obrigatórias nunca são `null` e os campos podem ser `final`. Com `@Autowired` em
campo, `new PedidoService()` compila e cria um objeto inválido, que só quebra com
`NullPointerException` quando alguém chama um método.

O construtor também documenta a classe. Quem lê a assinatura sabe do que ela precisa, e o teste
monta a classe com `new PedidoService(fake)`, sem Spring nem reflexão. Ele ainda deixa o custo
visível: um construtor com oito parâmetros incomoda, e esse incômodo avisa que a classe tem
responsabilidades demais (a documentação do Spring chama isso de code smell).

O argumento a favor da injeção em campo é escrever menos. É verdade, mas o que ela economiza é
justamente a parte que documenta a classe.

## Quando é aceitável

Em classes de teste com `@SpringBootTest`, onde quem cria a instância é o framework e ninguém
a constrói à mão, `@Autowired` em campo é comum e aparece na própria documentação do Spring (o
Shishō não analisa `src/test`). Em código legado, migrar aos poucos vale mais do que reescrever
tudo de uma vez. Dependências opcionais podem vir por setter, como a documentação do Spring
sugere, mas isso é raro numa aplicação Spring Boot.

Num projeto Quarkus, `@Inject` em campo package-private é o estilo que a própria documentação
recomenda: o ArC gera, na compilação, o código que preenche o campo, sem reflexão. O Shishō
reconhece o Quarkus pelo build (uma dependência `io.quarkus`) e, nele, só acusa `@Autowired`.

## Antes

```java
@Service
public class PedidoService {

    @Autowired
    private PedidoRepository pedidos;
}
```

## Depois

O construtor declara a dependência; `final` garante que ela não muda. Com um único
construtor, o Spring injeta sem precisar de `@Autowired`, e o teste instancia a classe com
`new PedidoService(fake)`.

```java
@Service
public class PedidoService {

    private final PedidoRepository pedidos;

    public PedidoService(PedidoRepository pedidos) {
        this.pedidos = pedidos;
    }
}
```

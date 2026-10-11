---
id: quarkus-private-injection
referencias:
  - https://quarkus.io/guides/cdi-reference#native-executables-and-private-members
  - https://quarkus.io/guides/writing-extensions#bootstrap-three-phases
---

## Por quê

O Quarkus monta a injeção de dependências na compilação, não na inicialização. O ArC, sua
implementação de CDI, lê os beans no build e gera classes Java que criam cada um e atribuem os
campos diretamente, como se alguém tivesse escrito `servico.pedidos = pedidos`. Essas classes
ficam no mesmo pacote do bean, então enxergam campos package-private, mas não campos `private`.

Para um campo `private`, o ArC volta à reflexão: mais trabalho na inicialização e, numa imagem
nativa (GraalVM), o campo precisa ser registrado para reflexão. O Quarkus faz o registro sozinho e
avisa no log ("unrecommended usage of private members"), mas o custo continua lá. Tirar o
`private` é a correção inteira.

Num projeto Spring a regra é outra, e a `field-injection` cuida dela: lá a recomendação é o
construtor.

## Quando é aceitável

Numa classe que você não pode mudar, ou quando o encapsulamento do campo importa mais do que
alguns milissegundos de inicialização numa aplicação que roda na JVM. Injeção por construtor
também evita o problema, e com ela o campo pode ser `private final`.

## Antes

```java
@ApplicationScoped
public class PedidoService {

    @Inject
    private PedidoRepository pedidos;
}
```

## Depois

Package-private: o código gerado pelo ArC atribui o campo sem reflexão.

```java
@ApplicationScoped
public class PedidoService {

    @Inject
    PedidoRepository pedidos;
}
```

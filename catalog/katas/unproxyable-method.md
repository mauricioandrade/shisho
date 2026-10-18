---
id: unproxyable-method
referencias:
  - https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html
  - https://docs.spring.io/spring-framework/reference/core/aop/proxying.html
---

## Por quê

`@Transactional`, `@Async` e `@Cacheable` não fazem nada sozinhas. O Spring cria um **proxy**, um
objeto que fica na frente do bean, e é o proxy que abre a transação, joga a chamada para outra
thread ou consulta o cache antes de chamar o método de verdade. Quem injeta o bean recebe o proxy.

O proxy é uma subclasse gerada em tempo de execução, e uma subclasse só consegue interceptar o que
ela pode sobrescrever. Um método `private` não é visível para ela, um `static` não pertence ao
objeto e um `final` não pode ser sobrescrito. Nos três casos a anotação é ignorada **sem nenhum
aviso**: o código compila, sobe e roda sem transação. O erro só aparece quando uma falha no meio do
método deixa metade dos dados gravada.

No Spring Boot 2 (Spring 5) a regra é mais estreita: o proxy só intercepta métodos `public`. O
Spring 6 passou a aceitar `protected` e package-private nos proxies de classe.

## Quando é aceitável

Nunca: a anotação nesse método não tem efeito, então ou ela sai, ou o método muda. A mesma
armadilha aparece quando um método do bean chama outro método anotado da **mesma classe**
(`salvar()` ou `this.salvar()`): a chamada vai direto ao objeto, sem passar pelo proxy, e a kata
também acusa. No `@Transactional`, ela só acusa quando quem chama ainda não está numa transação
(nem o método, nem a classe têm `@Transactional`), ou quando o método chamado pede uma
`propagation` própria, como `REQUIRES_NEW`, que por dentro nunca vale. A saída comum é mover o
método anotado para outro bean e chamá-lo por lá.

No Quarkus a kata não roda: desde o Quarkus 3, uma anotação de interceptor num método `private`
faz o build falhar, e o ArC intercepta métodos `static`.

## Antes

```java
@Service
public class PedidoService {

    public void fechar(Pedido pedido) {
        gravarItens(pedido);
    }

    @Transactional
    private void gravarItens(Pedido pedido) {
        pedido.itens().forEach(itens::save);
    }
}
```

## Depois

```java
@Service
public class PedidoService {

    @Transactional
    public void fechar(Pedido pedido) {
        gravarItens(pedido);
    }

    private void gravarItens(Pedido pedido) {
        pedido.itens().forEach(itens::save);
    }
}
```

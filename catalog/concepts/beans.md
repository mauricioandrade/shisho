---
id: beans
nome: Beans e estereótipos do Spring
grupo: spring
aparece_em:
  - anotacao: Service
  - anotacao: Component
  - anotacao: Repository
  - anotacao: Controller
  - anotacao: RestController
  - anotacao: Configuration
referencias:
  - https://docs.spring.io/spring-framework/reference/core/beans/introduction.html
  - https://docs.spring.io/spring-framework/reference/core/beans/classpath-scanning.html
---

## O que é

Um bean é um objeto que o Spring cria e gerencia para você. Anotações como `@Service`,
`@Repository` e `@RestController` (os estereótipos) marcam a classe: na inicialização, o Spring
a encontra, cria **uma** instância e a entrega a quem precisar dela.

O estereótipo também diz o papel da classe: `@Service` para regra de negócio, `@Repository` para
acesso a dados, `@RestController` para a porta HTTP. Por padrão, cada bean é um singleton
compartilhado por todas as requisições, por isso ele não deve guardar estado de uma requisição.

## Exemplo

```java
@Service
class CalculoDeFrete {

    BigDecimal calcular(Pedido pedido) {
        return pedido.peso().multiply(new BigDecimal("2.5"));
    }
}
```

---
id: lombok-constructor
referencias:
  - https://projectlombok.org/features/constructor
  - https://projectlombok.org/features/configuration
  - https://projectlombok.org/changelog
  - https://www.jetbrains.com/help/inspectopedia/SpringQualifierCopyableLombok.html
---

## Por quê

Usar Lombok não é errado, e muitos projetos sérios usam. A questão é o custo. O Lombok não é
uma biblioteca comum: é um processador de anotações que altera a árvore de sintaxe interna do
javac, uma API que o JDK não publica nem promete manter. Por isso cada versão nova do Java exige
uma versão nova do Lombok (o suporte ao JDK 25 chegou na 1.18.40), e atualizar o Java passa a
depender do calendário de outro projeto.

No caso do construtor, o código escondido é o mais importante da classe: a lista do que ela
precisa para existir. Esconder isso tem efeitos sutis. O `@Qualifier` (ou `@Value`, `@Lazy`)
que você põe no campo não é copiado para o parâmetro do construtor sem configurar
`lombok.copyableAnnotations` num `lombok.config`, e o Spring injeta o bean errado ou falha sem
que o código mostre por quê.

O ganho, hoje, é pequeno. Com um único construtor, o Spring injeta sem `@Autowired`, a IDE gera
o construtor num atalho e records resolvem os casos de dados. O que sobra é um construtor curto,
escrito uma vez.

## Quando é aceitável

Num projeto que já usa Lombok de forma consistente, com o time acostumado e o `lombok.config`
configurado, trocar pode não valer o esforço. Se for manter, prefira `@RequiredArgsConstructor`
a `@AllArgsConstructor`, que põe no construtor todo campo, inclusive os que não são
dependências. Se for trocar, o `shisho fix` escreve o construtor por você.

A kata só olha beans (`@Service`, `@Component`, `@ApplicationScoped`...). Numa classe de dados com
`@Data` e `@AllArgsConstructor`, o construtor guarda dados, não dependências; ali a pergunta é se
ela devia ser um record, e quem faz essa pergunta é a `dto-not-record`.

## Antes

```java
@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidos;
    private final Clock relogio;
}
```

## Depois

```java
@Service
public class PedidoService {

    private final PedidoRepository pedidos;
    private final Clock relogio;

    public PedidoService(PedidoRepository pedidos, Clock relogio) {
        this.pedidos = pedidos;
        this.relogio = relogio;
    }
}
```

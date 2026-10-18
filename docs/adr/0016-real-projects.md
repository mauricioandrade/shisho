# ADR 0016 — Calibração com projetos reais

- **Status:** aceito
- **Data:** 2026-10-18
- **Fase:** 16 (projetos reais)

## Contexto

Até a Fase 15, toda kata foi testada contra código que nós mesmos escrevemos: os fixtures e os
trechos dos testes. Esse código prova que a kata acha o que procura, mas não prova que ela fica
quieta no resto, porque quem escreveu o teste já sabia o que ela procurava. Para saber como o
Shishō se sai fora de casa, ele foi rodado em projetos open source conhecidos, e cada lição foi
conferida no código:

| Projeto | Build | O que exercita |
|---|---|---|
| `spring-projects/spring-petclinic` | Maven | Spring MVC com Thymeleaf |
| `spring-petclinic/spring-petclinic-microservices` | Maven, 8 módulos | multi-módulo, records |
| `gothinkster/spring-boot-realworld-example-app` | Gradle | Lombok em tudo, MyBatis, DGS |
| `jhipster/jhipster-sample-app` | Maven | o código que o JHipster gera |
| `thombergs/buckpal` | Gradle | hexagonal com estereótipos próprios |
| `ddd-by-examples/library` | Maven | DDD, testes de integração à parte |
| `quarkusio/quarkus-quickstarts` (9 deles) | Maven | Quarkus REST, Panache, validação |

Quase todos declaram Java 11 ou 17, e o Shishō os recusa (código de saída 3), como manda a
seção 2 do CLAUDE.md. Para analisá-los, a versão foi trocada para 21 numa cópia. O código continua
válido, porque Java 21 aceita tudo o que o 11 e o 17 aceitam.

## Decisão

Cada falso positivo virou uma correção com um teste feito do caso real (o nome das classes vem
do projeto onde ele apareceu):

| Kata | O que o projeto mostrou | Agora |
|---|---|---|
| `request-body-without-valid` | `PasswordChangeDTO` do JHipster não tem nenhuma restrição, e a lição dizia que "as anotações de validação do DTO são ignoradas" | só acusa quando a classe do corpo (ou a que ela estende) declara uma restrição; um tipo de fora do projeto continua acusado |
| `lombok-constructor` | `ArticleData` (`@Data @AllArgsConstructor`) do realworld recebia uma lição sobre dependências escondidas | só olha beans; classe de dados é assunto da `dto-not-record` |
| `lombok-constructor`, `non-final-dependency` | no buckpal, `@UseCase`, `@PersistenceAdapter` e `@WebAdapter` são `@Component`, e as katas não viam bean nenhum | um tipo de anotação do projeto anotado com um estereótipo também é estereótipo, transitivamente |
| `missing-controller-advice` | o petclinic renderiza Thymeleaf e recebia uma lição sobre ProblemDetail apontando para um controller que nunca devolve corpo | só conta controllers que respondem com dados (`@RestController`, `@ResponseBody`, `ResponseEntity`) |
| `missing-springdoc` | o mesmo petclinic | uma aplicação cujos controllers só renderizam views não tem API para documentar |
| `dto-not-record` | `PetDetailsBuilder`, aninhado no record `PetDetails` dos microservices | um builder nunca é DTO; num pacote `dto`, uma classe sem estado (mapper) também não |
| parser | no `library`, o `@Autowired` de um teste em `src/integration-test/java` virou lição GRAVE | pula qualquer source set que não seja o `main` (`src/<nome>/java`) |

As katas que olham outras classes (`request-body-without-valid`, `lombok-constructor`,
`non-final-dependency`) passaram de `ClassKata` a `ProjectKata`. É a mesma troca que a
`entity-in-controller` já fazia, pelo mesmo motivo: a resposta depende de uma classe que não é a
que está sendo avaliada.

### O que foi conferido e está certo

Também ficou registrado o que parecia suspeito e se mostrou correto, para ninguém "consertar"
depois:

- `transactional-read-only` nos GETs do JHipster: o `@Transactional` de escrita está na classe, e
  os métodos de leitura o herdam.
- Duas `entity-in-controller` na mesma linha do quickstart de Panache: o `update` recebe uma
  entidade (`Fruit`) e devolve outra (`FruitEntity`).
- `field-injection` nos `ConstraintValidator` do realworld: o Hibernate Validator os cria pela
  fábrica do Spring, que aceita injeção por construtor.
- `request-body-without-valid` no `validation-quickstart`, que valida à mão com
  `validator.validate(book)` de propósito: é o caso que o guia da kata já descreve como
  aceitável.

## Alternativas registradas

- **Um projeto fixture para cada caso real.** Os testes de cada kata já usam o trecho que causou
  o falso positivo, com o nome que ele tinha no projeto de origem. Um fixture inteiro repetiria
  isso com mais arquivos para manter.
- **Pular a lição quando o método valida à mão**, procurando `validator.validate(...)` no corpo
  dele. O modelo guarda as chamadas da classe, mas não diz em que método cada uma está; seria
  preciso guardar o fim de cada método só para este caso.
- **Aceitar projetos em Java 17** para valer em mais projetos reais. Fica para uma decisão de
  produto: a consciência de versão (seção 6 do CLAUDE.md) foi desenhada a partir do 21.

O que continua de fora: estereótipos de bibliotecas (`@DgsComponent` do Netflix DGS), que o
Shishō não tem como ver por dentro, e o detector de injeção por construtor do modo shoshin, que
ainda olha só os estereótipos conhecidos.

## 📚 Estudo — precisão e revocação

Uma regra que aponta problemas pode errar de dois jeitos. O **falso positivo** acusa o que está
certo; o **falso negativo** deixa passar o que está errado. A **precisão** mede o primeiro (das
lições dadas, quantas estavam certas), e a **revocação**, o segundo (dos problemas que existem,
quantos foram achados).

Para uma ferramenta que ensina, os dois erros não custam a mesma coisa. Um falso negativo é uma
lição a menos. Um falso positivo é uma lição errada, dada com a autoridade de quem ensina: o aluno
ou aprende a coisa errada, ou aprende que o mestre erra e para de ouvir. Por isso, na dúvida, o
Shishō prefere ficar quieto. Foi o que se fez aqui com os DTOs sem restrição e com os controllers
que só renderizam views.

O caso do buckpal mostra o outro lado. Limitar a `lombok-constructor` a beans melhorou a precisão,
mas criou um falso negativo nos estereótipos próprios, e só um projeto real mostrou isso. Cada
correção de um erro pode criar o erro oposto. É por isso que vale rodar de novo, nos mesmos
projetos, depois de cada mudança.

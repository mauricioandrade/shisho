# ADR 0012 — Quarkus

- **Status:** aceito
- **Data:** 2026-10-11
- **Fase:** 12 (Quarkus)

## Contexto

O Shishō nasceu para Spring Boot, mas boa parte dele já não depende do framework: a classificação
de arquitetura, o veredito, as regras de dependência e as katas de Java puro (`system-out`,
`dto-not-record`, `avoided-dependency`, as de recursos do Java) olham pacotes, tipos e o build.
O Quarkus é o outro grande framework Java de backend, e um projeto Quarkus já pode ser analisado.

O problema é que, num projeto Quarkus, o Shishō ensina errado. A kata `field-injection` acusa
`@Inject` em campo como GRAVE, e a documentação do Quarkus recomenda justamente isso: campo
`@Inject` package-private, sem construtor. Um projeto Quarkus exemplar sai com código 1 e quebra o
CI. As outras katas de Spring (`@Service`, `@RestController`, `@Value`...) só ficam em silêncio,
porque o Quarkus usa outras anotações para as mesmas ideias.

## Decisão

Suportar Quarkus em etapas, começando por não errar:

1. **12a — não errar.** O framework é reconhecido pelo build: um projeto Quarkus declara alguma
   dependência com `groupId` `io.quarkus` (`quarkus-arc`, `quarkus-rest`...). Nele,
   `field-injection` não acusa `@Inject`; `@Autowired` continua acusado. Um fixture
   `aluno-quarkus` exemplar é o teste-oráculo: nenhuma lição.
2. **12b — equivalências.** As katas que já existem passam a reconhecer o jeito Quarkus de fazer
   a mesma coisa: beans CDI (`@ApplicationScoped`, `@Singleton`...), Jakarta REST (`@Path`,
   `@GET`), `ExceptionMapper`, `@ConfigProperty`/`@ConfigMapping`, `smallrye-health` e
   `smallrye-openapi` no lugar de actuator e springdoc.
3. **12c — catálogo.** Panache (Active Record é uma escolha legítima, não domínio anêmico),
   conceitos do shoshin e katas que só existem no Quarkus, como injeção em campo `private`, que
   obriga o ArC a usar reflexão.

O reconhecimento fica nos adaptadores (`infrastructure/kata`), do mesmo jeito que
`WebApplications` reconhece uma aplicação web Spring. O `domain` não ganha um enum de framework:
cada kata pergunta ao build o que precisa saber.

Para isso, `field-injection` deixa de ser `ClassKata` e vira `ProjectKata`: uma classe sozinha
não diz se o projeto é Quarkus, e um `@Inject` do `jakarta.inject` também é aceito pelo Spring,
onde continua sendo injeção em campo.

### 12b — como as katas reconhecem o Quarkus

- **Pela anotação, quando ela basta.** `@ApplicationScoped`, `@POST` e `@ConfigProperty` só
  existem no mundo CDI/Jakarta REST/MicroProfile, então as katas de classe as reconhecem direto:
  `non-final-dependency` (beans CDI e resources `@Path`), `request-body-without-valid` e
  `entity-in-controller` (o corpo de um método `@POST` é o parâmetro sem anotação que não aparece
  no template do `@Path` nem é um tipo de contexto, como `UriInfo`), `missing-controller-advice`
  (`ExceptionMapper` ou `@ServerExceptionMapper`) e `prefer-configuration-properties`
  (`@ConfigProperty` repetido sugere `@ConfigMapping`).
- **Pelo build, quando só ele diz.** `missing-actuator` e `missing-springdoc` olham as extensões:
  um servidor Quarkus REST (`quarkus-rest*` ou `quarkus-resteasy*`, sem os clientes) sem
  `quarkus-smallrye-health` ou `quarkus-smallrye-openapi`.
- **O id da kata não muda.** `missing-actuator` continua com esse nome num projeto Quarkus,
  porque quem desliga a kata no `shisho.yaml` desliga a ideia (health check), não a biblioteca.
  O título passou a falar da ideia: "Aplicação web sem health check", "Corpo da requisição sem
  @Valid", "Propriedades repetidas com o mesmo prefixo".
- **A lição muda de texto.** Uma lição no jeito Quarkus usa a chave `kata.<id>.lesson.quarkus`,
  com as APIs do Quarkus. O `domain` não sabe disso: para ele, é só outra chave de mensagem.
- **Os guias ganham um parágrafo, não outro exemplo.** O "Antes/Depois" continua em Spring; o
  "Por quê" ou o "Quando é aceitável" explica o equivalente no Quarkus, com a fonte. Um segundo par
  de exemplos testados por kata dobraria o catálogo antes de alguém precisar dele.
- **O parâmetro ganhou linha** (`ClassUnit.Parameter.line`): no Jakarta REST o corpo não tem
  anotação, e a lição precisa apontar para algum lugar além do método.

Ficaram como estão, de propósito:

- `value-outside-constructor`: `@ConfigProperty` em campo é o estilo da documentação do Quarkus,
  pelo mesmo motivo do `@Inject`.
- `configuration-properties-not-record`: o `@ConfigMapping` é uma interface, não uma classe.
- `transactional-read-only`: o `@Transactional` do Jakarta não tem `readOnly`.
- `lombok-constructor`, `dto-not-record`: já não dependiam do framework.

### 12c — catálogo e katas próprias

- **Panache não precisou de exceção na classificação.** O que escondia um projeto Quarkus era o
  nome: o controller do Jakarta REST se chama `*Resource`, num pacote `resource`. Layered e
  package-by-feature passaram a aceitar `resource|recurso` onde aceitavam `controller|controlador`.
  Com isso, um Active Record sem repositório já casa com a assinatura (resource, service, entity).
  O `dominio_anemico` só olha pacotes `domain`, então uma entidade Panache com campos públicos em
  `entity` não é acusada.
- **`quarkus-private-injection`** (LEVE): `@Inject` ou `@ConfigProperty` em campo `private` num
  projeto Quarkus. O `shisho fix` tira o `private`. É a primeira kata que só existe num framework,
  e o prefixo `quarkus-` no id diz isso a quem lê o `shisho.yaml`.
- **`system-out`** virou `ProjectKata` para, num projeto Quarkus, sugerir `io.quarkus.logging.Log`.
- **Shoshin.** Um grupo Quarkus (beans CDI, resources Jakarta REST, Panache, `@ConfigMapping`) e
  o padrão Active Record. Entidade JPA, transações e Bean Validation saíram do grupo Spring para um
  grupo Jakarta EE, com textos que valem para os dois frameworks: num projeto Quarkus, o shoshin os
  mostrava sob o título "Spring". O Panache Repository conta como o padrão Repository.

O `Concept.Group` do `domain` já tinha `SPRING`; ganhou `QUARKUS` e `JAKARTA`. É um nome de
framework no `domain`, contra o que o CLAUDE.md pede, mas é um rótulo do catálogo (o título de um
grupo no relatório) e nenhuma regra depende dele. Trocá-lo por uma string vinda do catálogo é o
caminho se aparecer um terceiro framework.

## Alternativas registradas

- **Detectar o Quarkus pelos imports** (`io.quarkus.*`). Um bean Quarkus típico não importa nada
  do Quarkus: `@ApplicationScoped` e `@Inject` vêm do Jakarta. O build é o único lugar que diz.
- **Acusar só `@Inject` em campo `private` no Quarkus, como `field-injection`.** O conselho da
  kata (troque por construtor) seria errado para o Quarkus; o problema ali é a reflexão, e isso é
  outra kata (12c), com outra explicação e outra severidade.
- **Katas novas para o Quarkus** (`missing-smallrye-health`...). Dobrariam as katas para a mesma
  ideia, e quem desliga "health check" no `shisho.yaml` teria de desligar duas.
- **Um enum `Framework` no `ProjectModel`.** Seria uma abstração antes de haver duas katas que
  precisam dela, e colocaria nomes de framework no `domain`.

## Consequências

- Um projeto Quarkus deixa de receber uma lição GRAVE falsa por usar o estilo recomendado.
- ~~O Gradle continua sem leitor~~: desde o ADR 0013, `build.gradle(.kts)` também é lido, e um
  projeto Quarkus com Gradle é reconhecido como os com Maven.
- Os guias das katas que explicam a lição do jeito do Quarkus trazem também `## Antes (Quarkus)` e
  `## Depois (Quarkus)` (2026-10-15): uma lição `kata.<id>.lesson.quarkus` mostra esse exemplo no
  modo sensei, no HTML e no SARIF, em vez do de Spring.

## 📚 Estudo — injeção em tempo de build

O Spring monta o grafo de beans quando a aplicação sobe: lê as classes por reflexão, descobre os
construtores e os campos `@Autowired` e preenche tudo. Por isso, para o Spring, campo ou
construtor custam o mesmo, e a escolha é de design: o construtor deixa o objeto completo e
testável sem framework.

O Quarkus faz esse trabalho na compilação. O ArC lê as classes no build e gera código Java que
instancia os beans e atribui os campos diretamente, como se alguém tivesse escrito
`servico.repositorio = repositorio`. Para isso, o código gerado (no mesmo pacote) precisa enxergar
o campo: package-private funciona, `private` obriga a voltar à reflexão. É também o que permite
compilar para imagem nativa, onde reflexão precisa ser declarada.

O Quarkus também aceita injeção por construtor, e o argumento de design continua valendo lá. Mas
o framework documenta o campo como o estilo recomendado, e uma lição GRAVE contra a documentação
oficial faz o aluno desconfiar de todas as outras. Uma boa prática vale pelo contexto que a
justifica: quando o contexto muda, a kata precisa saber disso.

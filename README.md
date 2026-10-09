<div align="center">

# 師匠 Shishō

**O mestre que lê seu código, aponta o que está errado e explica por quê.**

Começando por Java e Spring Boot.

![Java 25](https://img.shields.io/badge/runs%20on-Java%2025-orange)
![Analisa Java 21+](https://img.shields.io/badge/analisa-Java%2021%2B-blue)
![License](https://img.shields.io/badge/license-Apache%202.0-green)

![Gojo Satoru, de Jujutsu Kaisen, puxando a venda dos olhos](docs/anime/hero.gif)

*Os Seis Olhos enxergam tudo. O Shishō enxerga o seu `@Autowired` em campo.*

</div>

---

## O que é

Linters dizem **o que** está errado. O Shishō é opinativo e didático: diz o que está errado,
**por que** importa e qual seria o caminho, como um sensei faria com um aluno.

![Ichigo, de Bleach, tomando suco de caixinha com cara de tédio absoluto](docs/anime/linters.gif)

*Você lendo 300 warnings de linter sem nenhuma explicação.*

- 🥋 **Katas** — regras de boas práticas Java/Spring Boot. Cada violação vira uma **lição**
  com explicação em português.
- 🏯 **Classificação de arquitetura** — reconhece se o projeto é layered, package-by-feature,
  monolito modular, hexagonal, clean, CQRS, event-driven ou microsserviços.
- ⚖️ **Veredito de overengineering** — cruza o que está no código com o contexto do projeto e
  diz se a arquitetura está grande demais (ou pequena demais) para o problema.
- 🎖️ **Graduação** — do 6º kyū ao 1º dan.
- 🧠 **LLM local opcional** — um modelo rodando na sua máquina explica o veredito. Ele explica,
  nunca decide.

## Demo

```
$ shisho analyze ./clareza-api

師匠 Shishō — análise de clareza-api  (Java 21)
Arquitetura: Camadas técnicas (Layered)  (83% da assinatura: controller, service, repository, model, dto)

✗ GRAVE  [field-injection]             UsuarioController.java:14
         Injeção em campo em usuarioService. Use construtor explícito com private final: a
         dependência fica visível e o objeto, imutável.
! MEDIA  [request-body-without-valid]  PedidoController.java:27
         @RequestBody CriarPedidoRequest sem @Valid: as anotações de validação do DTO nunca são
         verificadas.
! MEDIA  [entity-in-controller]        PedidoController.java:31
         O endpoint buscar() expõe a entidade JPA Pedido na API. Use records de request/response: o
         esquema do banco deixa de ser o contrato da API, campos internos não vazam e o cliente não
         consegue sobrescrevê-los.

Veredito: subengenharia. O projeto já não cabe na arquitetura Camadas técnicas (Layered):
  · PedidoService tem 31 métodos e 9 dependências no construtor.
Próximo passo natural: package-by-feature.

Graduação: 3º kyū  (8 lições: 1 grave, 4 médias, 3 leves)
```

<!-- TODO: substituir por docs/demo.gif gravado com vhs ou asciinema quando a Fase 1 estiver pronta -->

## A graduação

Cada lição desconta pontos. A nota final vira uma faixa:

| Nota | Graduação |
|---|---|
| 92–100 | 1º dan |
| 85–91 | 1º kyū |
| 75–84 | 2º kyū |
| 65–74 | 3º kyū |
| 55–64 | 4º kyū |
| 40–54 | 5º kyū |
| 0–39 | 6º kyū |

![Jiraiya, de Naruto, partindo um picolé de palito duplo ao meio e oferecendo metade ao Naruto](docs/anime/graduation.gif)

*Zero lições graves. O mestre divide o picolé.*

## Katas

| Kata | Severidade | O que aponta |
|---|---|---|
| `field-injection` | GRAVE | `@Autowired`/`@Inject` em campo |
| `lombok-constructor` | MEDIA | `@RequiredArgsConstructor`/`@AllArgsConstructor` escondendo as dependências |
| `non-final-dependency` | MEDIA | dependência injetada sem `final` |
| `system-out` | MEDIA | `System.out`, `System.err`, `printStackTrace()` |
| `request-body-without-valid` | MEDIA | `@RequestBody` sem `@Valid` |
| `entity-in-controller` | MEDIA | endpoint que recebe ou devolve uma `@Entity` JPA |
| `missing-controller-advice` | MEDIA | controllers sem nenhum `@RestControllerAdvice` no projeto |
| `value-outside-constructor` | LEVE | `@Value` fora de parâmetro de construtor |
| `dto-not-record` | LEVE | DTO (por pacote ou sufixo) que não é record |
| `transactional-read-only` | LEVE | método de leitura (`find…`, `buscar…`) numa transação de escrita |
| `configuration-properties-not-record` | LEVE | `@ConfigurationProperties` numa classe com setters |
| `prefer-configuration-properties` | LEVE | 3 ou mais `@Value` com o mesmo prefixo na mesma classe |
| `missing-actuator` | LEVE | aplicação web sem `spring-boot-starter-actuator` |
| `missing-springdoc` | INFO | aplicação web sem springdoc-openapi (só sugestão, não tira nota) |

Cada projeto tem suas razões. Um `shisho.yaml` na raiz do projeto analisado desliga katas ou
muda a severidade delas:

```yaml
# shisho.yaml
katas:
  system-out:
    ativa: false          # desliga a kata
  dto-not-record:
    severidade: info      # grave | media | leve | info
```

Violações das regras de dependência da arquitetura detectada aparecem como a kata
`dependency-rule` (MEDIA) e podem ser configuradas da mesma forma.

Kata desconhecida ou campo com erro de digitação faz a análise falhar (código de saída 3): é
melhor do que uma configuração que, em silêncio, não faz nada.

## Overengineering

A pergunta que o Shishō mais gosta de responder: **essa arquitetura cabe nesse projeto?**

Overengineering não é uma propriedade do código sozinho, é a relação entre a complexidade da
solução e a do problema. Por isso o Shishō junta duas coisas: os **fatos medidos** no código
(um use case por operação de CRUD, entidade JPA idêntica ao domínio, mappers entre estruturas
iguais, um service que faz tudo…) e o **contexto** que você declara:

```yaml
# shisho.yaml
contexto:
  tipo: mvp              # mvp | portfolio | produto | corporativo
  time: 1                # pessoas no projeto (10 ou mais conta como time grande)
  vida_util: curta       # curta | media | longa
  caracteristicas:       # o que mais justifica uma arquitetura maior, nas chaves do catálogo
    - dominio_com_regras_complexas
```

Primeiro o Shishō reconhece a arquitetura pela estrutura de pacotes e confere as regras de
dependência dela (o `domain` importando Spring vira lição). Depois vem o veredito:

```
Arquitetura: Clean Architecture  (100% da assinatura: domain, application, infrastructure, usecase, domain/model, domain/port)

Veredito: overengineering. A arquitetura Clean Architecture custa de 7 a 12 classes por
funcionalidade, e nada no contexto declarado paga esse custo. Ela vale a pena quando:
dominio_com_regras_complexas, vida_util_longa, troca_provavel_de_infra, time_grande.
  · Um mapper converte UsuarioJpaEntity em Usuario, que têm exatamente os mesmos campos.
  · Usuario e a entidade JPA UsuarioJpaEntity têm exatamente os mesmos campos: duas cópias da mesma
    estrutura, mais o mapeamento entre elas.
  · 4 use cases só para operações CRUD de Usuario: um use case por método, sem regra de negócio que
    justifique cada um.
Considere package-by-feature.
```

O caminho inverso também existe: um projeto em camadas com um service de 30 métodos ou services
que dependem um do outro recebe **subengenharia** e a sugestão do próximo degrau.

![Dazai, de Bungou Stray Dogs, entalado de cabeça para baixo dentro de um tambor, com cara de sofrimento](docs/anime/overengineering.gif)

*Doze classes para um CRUD. Entrar foi fácil.*

O conhecimento sobre cada arquitetura fica em [`catalog/architectures`](catalog/architectures),
em Markdown, com quando ela vale o custo e quando é exagero.

## O LLM explica, não julga

Tudo que o Shishō decide é determinístico: mesma entrada, mesmo resultado, pronto para CI. O
LLM local (via [Ollama](https://ollama.com)) é **opcional e vem desligado**. Quando ligado,
ele recebe os fatos e o veredito já calculados e só escreve a explicação:

```
$ shisho ask "por que não Clean Architecture aqui?"
```

![Gintoki, de Gintama, de jaleco escrevendo o próprio nome no quadro enquanto a turma chama "Sensei!"](docs/anime/sensei.gif)

*O Ginpachi-sensei dá a aula. A nota, quem dá é o Java.*

## Versões do Java

O Shishō roda em **Java 25** e analisa projetos a partir do **Java 21**. Ele lê a versão do seu
projeto e **só sugere o que ela permite**. Recursos em preview nunca são recomendados sem
você pedir.

![Goku Super Saiyajin, de Dragon Ball Z, virando para a câmera com um sorriso e fazendo joinha](docs/anime/java-versions.gif)

*Subiu o projeto para o Java 25? O Shishō desbloqueia as katas novas.*

## Roadmap

- [x] **Fase 1** — CLI, catálogo, 7 katas de AST, relatório no terminal
- [x] **Fase 2** — katas de projeto (JPA, transações, tratamento de erros, build,
  configuração) e `shisho.yaml`
- [x] **Fase 3** — classificação de arquitetura, regras de dependência e veredito de
  overengineering/subengenharia
- [x] **Fase 4** — relatório HTML, SARIF e GitHub Action
- [ ] **Fase 5** — exemplos antes/depois, catálogo de recursos do Java, autofix com OpenRewrite
- [ ] **Fase 6** — LLM local opcional
- [ ] **Depois** — outras stacks, reaproveitando o catálogo de arquiteturas
- [ ] **Ideias** — modo `--shoshin` (初心, "mente de principiante"), que explica cada conceito
  que aparece no código (o que é um record, uma sealed interface…); gerador de CRUD que segue a
  arquitetura detectada no projeto e já passa em todas as katas

## Rodando a partir do código

Você precisa de **JDK 25** e **Maven 3.9**. Se já tem os dois, pule direto para o `mvn verify`.

Se não tem (ou tem outra versão do Java instalada para outros projetos), o repositório traz um
[`mise.toml`](mise.toml) com as versões exatas. O [mise](https://mise.jdx.dev) é um gerenciador
de versões de ferramentas — parecido com SDKMAN, asdf ou nvm, mas para várias linguagens ao
mesmo tempo. Ele lê o `mise.toml` e, **só dentro desta pasta**, coloca no `PATH` o Java e o
Maven certos, sem mexer no que você já usa no resto da máquina.

```bash
# instale o mise: https://mise.jdx.dev/getting-started.html
mise trust      # autoriza o mise.toml deste repositório (só na primeira vez)
mise install    # baixa o Temurin 25 e o Maven pinados
mvn verify      # testes + target/shisho.jar
```

Depois é só apontar o Shishō para a raiz de um projeto Maven:

```bash
java -jar target/shisho.jar analyze ~/projetos/minha-api
```

Além do terminal, o relatório sai em SARIF (para o code scanning do GitHub e outras
ferramentas) ou em HTML (uma página só, sem dependências, com tema claro e escuro):

```bash
java -jar target/shisho.jar analyze --format sarif --output shisho.sarif ~/projetos/minha-api
java -jar target/shisho.jar analyze --format html  --output shisho.html  ~/projetos/minha-api
```

O código de saída foi pensado para CI e não depende do formato:

| Código | Significado |
|---|---|
| 0 | nenhuma lição grave |
| 1 | há pelo menos uma lição **GRAVE** |
| 2 | entrada inválida (caminho inexistente, argumento faltando, `--output` que não pode ser gravado) |
| 3 | o projeto não pôde ser analisado (ex.: Java abaixo do 21, `shisho.yaml` inválido) |

O mise é usado só para conveniência de desenvolvimento. Ele não é dependência do Shishō, e
nada no build exige que você o use.

## No GitHub Actions

```yaml
jobs:
  shisho:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      security-events: write   # para enviar o SARIF ao code scanning
    steps:
      - uses: actions/checkout@v4
      - uses: mauricioandrade/shiso@main
        with:
          path: .                # projeto a analisar (padrão: raiz do repositório)
          fail-on-grave: true    # reprova o job se houver lição GRAVE (padrão: true)
          upload-sarif: true     # lições como alertas do code scanning (padrão: true)
          html: shisho.html      # opcional: também grava o relatório HTML
```

As lições aparecem como alertas no code scanning e anotações no pull request; o relatório do
terminal vai para o resumo do job. A action instala o Java 25 e compila o Shishō a partir do
código dela mesma. Como isso troca o Java do job, **rode-a num job separado** do build do seu
projeto.

## Contribuindo

O jeito mais fácil de contribuir não exige escrever Java: **o catálogo é Markdown**. Uma
arquitetura nova, um sinal de overengineering que faltou, uma explicação mais clara — tudo é
um PR em `catalog/`. O schema está em [`catalog/README.md`](catalog/README.md).

![Naruto fazendo o selo de mão do Kage Bunshin no Jutsu](docs/anime/contributing.gif)

*Um dev sozinho não cobre todas as katas. Precisamos de clones.*

Issues marcadas com `good first issue` são um bom ponto de partida.

## Licença

Código sob [Apache 2.0](LICENSE). Os GIFs de anime pertencem aos seus criadores e estúdios
e são usados apenas como ilustração.

## Créditos

- [mise](https://github.com/jdx/mise), de Jeff Dickey e contribuidores (MIT) — gerenciamento
  das versões de Java e Maven no desenvolvimento.
- [JavaParser](https://github.com/javaparser/javaparser) (Apache 2.0 / LGPL) — parser de
  código Java usado pelas katas. A escolha está em [`docs/adr/0001-parser.md`](docs/adr/0001-parser.md).
- [picocli](https://picocli.info) (Apache 2.0) — linha de comando.
- [SnakeYAML](https://bitbucket.org/snakeyaml/snakeyaml) (Apache 2.0) — leitura do frontmatter
  do catálogo.

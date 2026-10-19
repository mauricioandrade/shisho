<div align="center">

# 師匠 Shishō

**O mestre que lê seu código, aponta o que está errado e explica por quê.**

Começando por Java, com Spring Boot e Quarkus.

[![CI](https://github.com/mauricioandrade/shisho/actions/workflows/ci.yml/badge.svg)](https://github.com/mauricioandrade/shisho/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/mauricioandrade/shisho?label=release)](https://github.com/mauricioandrade/shisho/releases)
![Roda em Java 25](https://img.shields.io/badge/roda%20em-Java%2025-orange?logo=openjdk)
![Analisa Java 8+](https://img.shields.io/badge/analisa-Java%208%2B-blue?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3%20%7C%204-6DB33F?logo=springboot&logoColor=white)
![Quarkus](https://img.shields.io/badge/Quarkus-3-4695EB?logo=quarkus&logoColor=white)
[![Licença Apache 2.0](https://img.shields.io/badge/licen%C3%A7a-Apache%202.0-green)](LICENSE)
![Idiomas: pt-BR e en](https://img.shields.io/badge/idiomas-pt--BR%20%7C%20en-informational)
[![Plugin para o Claude Code](https://img.shields.io/badge/Claude%20Code-plugin-D97757)](#-no-claude-code)

**Português** · [English](README.en.md)

![Gojo Satoru, de Jujutsu Kaisen, puxando a venda dos olhos](docs/anime/hero.gif)

*Os Seis Olhos enxergam tudo. O Shishō enxerga o seu `@Autowired` em campo.*

</div>

---

**Começando:** [O que é](#-o-que-é) · [Por que existe](#-por-que-existe) · [Instalação](#-instalação) · [Uso rápido](#-uso-rápido) · [Demo](#-demo)

**Guia:** [Katas](#-katas) · [A graduação](#-a-graduação) · [Modo sensei](#-modo-sensei-aprender-e-corrigir) · [Modo shoshin](#-modo-shoshin-os-conceitos-do-seu-código) · [Overengineering](#-overengineering) · [`shisho crud`](#-do-zero-shisho-crud) · [LLM local](#-o-llm-explica-não-julga) · [No Claude Code](#-no-claude-code) · [Versões do Java](#-versões-do-java) · [Relatórios](#-relatórios-e-códigos-de-saída) · [No GitHub Actions](#-no-github-actions)

**Projeto:** [Desenvolvimento](#-desenvolvimento) · [Roadmap](#-roadmap) · [Contribuindo](#-contribuindo) · [Licença](#-licença) · [Créditos](#-créditos)

---

## 🏯 O que é

Linters dizem **o que** está errado. O Shishō é opinativo e didático: diz o que está errado,
**por que** importa e qual seria o caminho, como um sensei faria com um aluno.

![Ichigo, de Bleach, tomando suco de caixinha com cara de tédio absoluto](docs/anime/linters.gif)

*Você lendo 300 warnings de linter sem nenhuma explicação.*

- 🥋 **Katas** — regras de boas práticas Java, Spring Boot e Quarkus. Cada violação vira uma **lição**
  com explicação, em português ou em inglês.
- 🏯 **Classificação de arquitetura** — reconhece se o projeto é layered, package-by-feature,
  monolito modular, hexagonal, clean, CQRS, event-driven ou microsserviços.
- ⚖️ **Veredito de overengineering** — cruza o que está no código com o contexto do projeto e
  diz se a arquitetura está grande demais (ou pequena demais) para o problema.
- 🎖️ **Graduação** — do 6º kyū ao 1º dan.
- 🌱 **Modo shoshin** — dá nome aos conceitos que já estão no código (record, transações, Repository, Strategy…) e explica cada um.
- 🧠 **LLM local opcional** — um modelo rodando na sua máquina explica o veredito. Ele explica,
  nunca decide.
- 🏗️ **Gerador de CRUD** — cria um CRUD REST na arquitetura do projeto, explicando o papel de
  cada arquivo. O código gerado passa em todas as katas.
- 🤖 **Plugin para o Claude Code** — confere cada arquivo que o Claude edita e mostra a ele as
  lições, enquanto ele escreve.

## 💡 Por que existe

- **Linters apontam, não ensinam.** Um warning diz que algo está errado, mas quem está
  aprendendo precisa do porquê, do custo e de quando a regra não vale. O Shishō escreve a lição
  inteira.
- **Arquitetura demais também é erro.** Muito tutorial ensina hexagonal ou Clean Architecture
  para um CRUD de três campos, e nenhuma ferramenta pergunta se a arquitetura cabe no projeto. O
  Shishō pergunta, e responde com fatos medidos no código.
- **Determinístico e local.** Mesma entrada, mesmo resultado: dá para rodar em CI. Seu código não
  sai da máquina, nem quando você usa o LLM, que roda localmente e é opcional.

## 🚀 Instalação

Você só precisa do **Java 25** ([Temurin](https://adoptium.net), por exemplo). O instalador baixa
o `shisho.jar` da última [release](https://github.com/mauricioandrade/shisho/releases), confere o
checksum e pergunta o idioma:

```bash
bash -c "$(curl -fsSL https://raw.githubusercontent.com/mauricioandrade/shisho/main/install.sh)"
```

O jar vai para `~/.local/share/shisho/` e o comando `shisho` para `~/.local/bin/`. Se você deixar,
o instalador põe essa pasta no `PATH` e grava o `SHISHO_IDIOMA` (`pt` ou `en`) no seu `~/.bashrc`
ou `~/.zshrc`; rodar de novo atualiza a versão e substitui as duas linhas. Em português ou em
inglês, o idioma vale para tudo: relatórios, guias do modo sensei e o código que o `shisho crud`
gera. Para trocar só num comando: `SHISHO_IDIOMA=en shisho analyze .`.

Prefere baixar à mão? O `shisho.jar` da release roda com `java -jar shisho.jar analyze <projeto>`.

O código analisado pode estar em português ou em inglês: o Shishō reconhece pacotes como
`dominio`, `servico` e `porta/entrada` tanto quanto `domain`, `service` e `port/in`.

![Gojo Satoru, de Jujutsu Kaisen, em versão chibi, saltitando feliz num campo de flores](docs/anime/install.gif)

*Um comando, uma pergunta, e o Shishō está pronto.*

## ⚡ Uso rápido

Aponte para a raiz de um projeto Maven ou Gradle com Java 8 ou superior (num multi-módulo, a raiz que lista os
módulos):

| Comando | O que faz |
|---|---|
| `shisho analyze ./meu-projeto` | lições, arquitetura, veredito e graduação |
| `shisho analyze --sensei ./meu-projeto` | o mesmo, com o porquê e o antes/depois de cada lição |
| `shisho analyze --shoshin ./meu-projeto` | explica os conceitos do código: record, injeção de dependência, Strategy… |
| `shisho analyze --format html --output r.html ./meu-projeto` | relatório HTML (ou `sarif`, para o GitHub) |
| `shisho fix ./meu-projeto` | mostra as correções automáticas; `--apply` grava |
| `shisho baseline ./meu-projeto` | grava as lições de hoje; daí em diante, o `analyze` mostra e reprova só as novas |
| `shisho crud --arquitetura layered ./meu-projeto Produto nome:String` | cria um CRUD que explica cada arquivo |
| `shisho ask ./meu-projeto "por onde começo?"` | um LLM local explica o veredito (precisa do Ollama) |
| `/plugin install shisho --marketplace mauricioandrade/shisho` | no Claude Code: o Shishō confere cada arquivo que o Claude edita |

`shisho <comando> --help` lista todas as opções.

## 🎬 Demo

![Gravação do terminal: shisho analyze aponta quatro lições, shisho fix --apply corrige três e a nova análise dá 1º dan](docs/demo/demo.gif)

Um projeto com quatro lições: o Shishō aponta cada uma e explica o porquê, o `fix --apply`
corrige três sozinho, e a graduação sobe de 2º kyū para 1º dan.

<details>
<summary>A saída em texto, num projeto maior</summary>

```
$ shisho analyze ./clareza-api

師匠 Shishō — análise de clareza-api  (Java 21)
Arquitetura: Camadas técnicas (Layered)  (83% da assinatura: controller, service, repository, model, dto)

✗ GRAVE  [field-injection]             UsuarioController.java:14
         usuarioService é injetado direto no campo. Assim, new cria a classe sem a dependência, o
         erro só aparece como NullPointerException em uso e o teste precisa de Spring ou reflexão.
         Receba a dependência pelo construtor, num campo private final.
! MEDIA  [request-body-without-valid]  PedidoController.java:27
         @RequestBody CriarPedidoRequest sem @Valid: as anotações de validação do DTO são ignoradas,
         e dados inválidos chegam ao service como se fossem válidos. Adicione @Valid antes do
         @RequestBody.
! MEDIA  [entity-in-controller]        PedidoController.java:31
         O endpoint buscar() expõe a entidade JPA Pedido na API. Use records de request/response: o
         esquema do banco deixa de ser o contrato da API, campos internos não vazam e o cliente não
         consegue sobrescrevê-los.

Veredito: subengenharia. O projeto já não cabe na arquitetura Camadas técnicas (Layered):
  · PedidoService tem 31 métodos e 9 dependências no construtor.
Próximo passo natural: package-by-feature.

Graduação: 3º kyū  (8 lições: 1 grave, 4 médias, 3 leves)
```

</details>

## 🥋 Katas

| Kata | Severidade | O que aponta | Por que importa |
|---|---|---|---|
| `field-injection` | GRAVE | `@Autowired`/`@Inject` em campo (no Quarkus, só `@Autowired`) | `new` cria o objeto incompleto, e o teste precisa de Spring |
| `unproxyable-method` | GRAVE | `@Transactional`, `@Async` ou `@Cacheable` em método `private`, `static` ou `final` (no Boot 2, não `public`), ou chamado de dentro da própria classe | o proxy não alcança o método, e a anotação é ignorada em silêncio |
| `entity-without-default-constructor` | GRAVE | `@Entity`/`@Embeddable` cujos construtores recebem todos parâmetros | o Hibernate não consegue criar a entidade, e a primeira consulta falha |
| `destructive-schema-generation` | GRAVE | `ddl-auto=create-drop` (no Quarkus, `drop-and-create`) fora dos perfis de desenvolvimento | cada partida apaga as tabelas, com os dados junto |
| `lombok-constructor` | MEDIA | `@RequiredArgsConstructor`/`@AllArgsConstructor` | as dependências somem do código, e a décima entra sem ninguém notar |
| `non-final-dependency` | MEDIA | dependência injetada sem `final` | qualquer método pode trocá-la depois da construção |
| `system-out` | MEDIA | `System.out`, `System.err`, `printStackTrace()` | sem nível nem horário, e fora do agregador de logs |
| `swallowed-exception` | MEDIA | `catch` sem instrução nem comentário | o erro some sem rastro e aparece depois, longe da causa |
| `request-body-without-valid` | MEDIA | corpo da requisição sem `@Valid` (`@RequestBody` ou corpo Jakarta REST) | a validação do DTO é ignorada, e dado inválido chega ao service |
| `entity-in-controller` | MEDIA | endpoint que recebe ou devolve uma `@Entity` JPA | o banco vira o contrato da API, e campos internos vazam |
| `entity-with-lombok-data` | MEDIA | `@Entity` com `@Data` ou `@EqualsAndHashCode` de todos os campos | o hash muda no `persist`, e o `toString` percorre coleções lazy |
| `eager-collection` | MEDIA | `@OneToMany`/`@ManyToMany` com `fetch = EAGER` | a coleção vem em toda consulta: N+1 ou produto cartesiano |
| `hardcoded-secret` | MEDIA | senha, segredo, token ou chave escrito na configuração | quem lê o repositório lê o segredo, e o Git não esquece |
| `open-in-view` | MEDIA | Spring Data JPA sem `spring.jpa.open-in-view` declarado (o padrão é `true`) | cada requisição segura uma conexão, e consultas escapam do service |
| `missing-controller-advice` | MEDIA | controllers sem nenhum `@RestControllerAdvice` (no Quarkus, `ExceptionMapper`) | cada endpoint trata (ou esquece) os próprios erros |
| `avoided-dependency` | MEDIA | dependência abandonada, substituída ou movida (Log4j 1, Springfox, Joda-Time... ver [`catalog/dependencies`](catalog/dependencies)) | sem correções de segurança, ou já substituída pelo próprio projeto |
| `value-outside-constructor` | LEVE | `@Value` fora de parâmetro de construtor | o valor chega por reflexão, e um teste com `new` fica sem ele |
| `dto-not-record` | LEVE | DTO (por pacote ou sufixo) que não é record | getters, `equals` e `hashCode` escritos à mão, e mutável |
| `transactional-read-only` | LEVE | método de leitura (`find…`, `buscar…`) numa transação de escrita | o Hibernate faz dirty checking à toa |
| `to-one-without-lazy` | LEVE | `@ManyToOne`/`@OneToOne` sem `fetch` (o padrão do JPA é EAGER) | cada entidade carregada traz a outra junto, e uma lista vira N+1 |
| `transactional-controller` | LEVE | `@Transactional` num `@Controller`/`@RestController` | a transação e a regra que ela protege ficam fora do service |
| `sql-logging` | LEVE | `show-sql` (no Quarkus, `log.sql`) ou o logger `org.hibernate.SQL` em debug fora dos perfis de desenvolvimento | o log cresce a cada requisição e leva dados junto |
| `configuration-properties-not-record` | LEVE | `@ConfigurationProperties` numa classe com setters | a configuração pode ser alterada depois de carregada |
| `prefer-configuration-properties` | LEVE | 3 ou mais `@Value` (ou `@ConfigProperty`) com o mesmo prefixo na mesma classe | propriedades soltas, sem tipo nem validação num lugar só |
| `missing-actuator` | LEVE | aplicação web sem `spring-boot-starter-actuator` (no Quarkus, `quarkus-smallrye-health`) | sem health check, o orquestrador não sabe se a aplicação está viva |
| `missing-springdoc` | INFO | aplicação web sem springdoc-openapi (no Quarkus, `quarkus-smallrye-openapi`) | quem consome a API não tem contrato nem Swagger UI |
| `prefer-virtual-threads` | INFO | `Executors.newFixedThreadPool`/`newCachedThreadPool` (Java 21) | para I/O, virtual threads dispensam dimensionar pool |
| `prefer-scoped-values` | INFO | campo `ThreadLocal` (Java 25) | `ScopedValue` não vaza entre tarefas e não precisa de `remove()` |
| `quarkus-private-injection` | LEVE | campo `private` com `@Inject` ou `@ConfigProperty` (só Quarkus) | o ArC volta à reflexão, que custa na inicialização e na imagem nativa |

As quatro katas de configuração leem o `application.properties` e o `application.yml` de cada
módulo, com os perfis (`application-dev.yml`, `%dev.` no Quarkus). O que vale só para
desenvolvimento ou teste não é acusado ([ADR 0024](docs/adr/0024-settings-katas.md)).

Um projeto Quarkus é reconhecido pelo build (uma dependência `io.quarkus`): as katas usam os
equivalentes dele (CDI, Jakarta REST, SmallRye) e explicam a lição com as APIs do Quarkus. Por
quê e como: [ADR 0012](docs/adr/0012-quarkus.md).

As sugestões (INFO) não tiram nota. Com `--sensei`, cada lição vem com a explicação completa, os
casos em que o código acusado é aceitável e o exemplo antes/depois.

![Sung Jinwoo, de Solo Leveling, com um olho brilhando em azul e um meio sorriso](docs/anime/katas.gif)

*Ninguém começa no rank S. Cada lição corrigida é experiência.*

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

## 🏅 A graduação

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

## 📖 Modo sensei: aprender e corrigir

Com `--sensei`, o relatório explica cada kata encontrada: **por que** ela existe, **quando** o
código acusado é uma escolha razoável, o código **antes e depois** da correção e as **fontes**
(documentação do Spring, JEPs, OWASP). Uma kata aponta um custo, não um pecado: o sensei mostra
a troca para você decidir. Os guias ficam em [`catalog/katas`](catalog/katas), e um teste
garante que o "antes" gera a lição e o "depois" não. O relatório HTML e a ajuda dos alertas no
GitHub mostram o mesmo conteúdo.

```bash
java -jar target/shisho.jar analyze --sensei ~/projetos/minha-api
```

Parte das lições tem uma correção mecânica segura, e o Shishō faz por você:

```bash
java -jar target/shisho.jar fix ~/projetos/minha-api            # mostra o diff
java -jar target/shisho.jar fix --apply ~/projetos/minha-api    # grava
```

![Joseph Joestar, de JoJo's Bizarre Adventure, com o Hermit Purple em punho e o olhar de quem já sabe o que vem](docs/anime/autofix.gif)

*"Sua próxima linha vai ser: `private final UsuarioService service;`" O `fix` mostra o diff antes de gravar.*

| Kata | O que o `fix` faz |
|---|---|
| `field-injection` | tira o `@Autowired`, deixa o campo `final` e o recebe pelo construtor |
| `lombok-constructor` | troca a anotação pelo construtor que o Lombok geraria |
| `non-final-dependency` | adiciona `final`, se todo construtor atribui o campo e nada mais o altera |
| `request-body-without-valid` | adiciona `@Valid`, se o build tem Bean Validation |
| `transactional-read-only` | coloca `readOnly = true` |
| `quarkus-private-injection` | tira o `private` do campo injetado |

Só as linhas da correção mudam: formatação e comentários ficam como estavam. Quando a correção
não é segura (um `@Qualifier` que teria de mudar de lugar, um `@Builder` que depende do
construtor do Lombok…), a lição fica para você, com o exemplo do modo sensei. Por quê
JavaParser e não OpenRewrite: [ADR 0003](docs/adr/0003-autofix.md).

## 🌱 Modo shoshin: os conceitos do seu código

O modo sensei explica o que está errado. O shoshin (初心, a "mente de principiante" do zen)
explica o que **já está** no código: com `--shoshin`, o relatório dá nome a cada conceito que
aparece no projeto, diz onde ele está e o que ele é.

```
$ shisho analyze --shoshin ./loja
...
初心 Conceitos neste código (modo shoshin)

Java
  Record  (4 lugares: ClienteRequest.java:5, ClienteResponse.java:3, ProdutoRequest.java:8 e mais 1)
    Uma classe feita só para carregar dados. Você declara os componentes entre parênteses, e o
    compilador escreve o resto: construtor, acessores (`nome()`, sem o `get`), `equals`, `hashCode`
    e `toString`. Os componentes são `final`: depois de criado, um record não muda.
    [...]

Spring
  Injeção de dependência pelo construtor  (4 lugares: ClienteController.java:23, ClienteService.java:14, ...)
  [...]

Jakarta EE
  Transações (@Transactional)  (8 lugares: ClienteService.java:9, ClienteService.java:26, ...)
  [...]

Padrões de projeto
  Repository  (2 lugares: ClienteRepository.java:5, ProdutoRepository.java:5)
  [...]
```

São 23 conceitos em cinco grupos, na ordem em que um iniciante deveria encontrá-los:

| Grupo | Conceitos |
|---|---|
| Java | record, enum, tipos selados, `Optional`, streams |
| Spring | beans, injeção de dependência, controller REST, tratamento de erros, `@ConfigurationProperties` |
| Quarkus | beans CDI e escopos, resources Jakarta REST, Panache, `@ConfigMapping` |
| Jakarta EE | entidade JPA, transações, Bean Validation |
| Padrões de projeto | Repository, Active Record, DTO, Strategy, Builder, Observer |

Rode num projeto gerado pelo `shisho crud` para ver cada peça explicada. O shoshin descreve, não
julga: um Strategy reconhecido não é elogio, e quem diz se ele cabe é o veredito. Os conceitos
ficam em [`catalog/concepts`](catalog/concepts), em Markdown, e um teste exige que cada um
reconheça o próprio exemplo. Por que assim: [ADR 0009](docs/adr/0009-shoshin.md).

## 📐 Overengineering

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

Quando o contexto justifica a arquitetura mas o código ainda mostra os sinais, o veredito é
**"overengineering, por enquanto"**: ele diz que motivo do `shisho.yaml` paga o custo, mostra o
que o código ainda não usa e não sugere trocar de arquitetura, porque a escolha já foi feita.

O caminho inverso também existe: um projeto em camadas com um service de 30 métodos ou services
que dependem um do outro recebe **subengenharia** e a sugestão do próximo degrau.

![Dazai, de Bungou Stray Dogs, entalado de cabeça para baixo dentro de um tambor, com cara de sofrimento](docs/anime/overengineering.gif)

*Doze classes para um CRUD. Entrar foi fácil.*

O conhecimento sobre cada arquitetura fica em [`catalog/architectures`](catalog/architectures),
em Markdown, com quando ela vale o custo e quando é exagero.

## 🧱 Do zero: `shisho crud`

O Spring Initializr resolve o build e as dependências, mas não diz onde cada classe vai nem por
quê. O `shisho crud` cria um CRUD REST completo (entidade, repositório, DTOs, service,
controller e tratamento de erros) na arquitetura do projeto, e explica o papel de cada arquivo:

```
$ shisho crud --arquitetura package-by-feature ./loja Produto nome:String preco:BigDecimal
師匠 Shishō — CRUD de Produto em Pacotes por funcionalidade (Package by Feature)

+ src/main/java/com/exemplo/loja/produto/Produto.java
  A entidade JPA: a tabela do banco vista como objeto. Como todas as classes de `produto`, ela é
  package-private (sem `public`): nenhuma outra feature enxerga a entidade, e as features
  conversam pelo service. [...]

+ src/main/java/com/exemplo/loja/produto/ProdutoService.java
  Onde mora a regra de negócio e onde começa a transação. A classe inteira é `readOnly = true`, e
  só os métodos que escrevem abrem uma transação de escrita. [...]
...
Criados 8 arquivos. Rode shisho analyze para conferir: o código gerado passa em todas as katas.
```

![Sasuke, de Naruto, com o Sharingan ativado e um sorriso de canto](docs/anime/crud.gif)

*O Sharingan copia o jutsu e entende cada selo. O `crud` copia a arquitetura do projeto e explica cada arquivo.*

- **Arquitetura:** `layered`, `package-by-feature` ou `hexagonal`. Sem `--arquitetura`, vale a que o Shishō
  detecta no projeto. Num projeto recém-saído do Initializr ainda não há o que detectar, então o
  primeiro `crud` precisa da opção. Se você pede uma arquitetura diferente da do projeto, ele
  gera e avisa que os estilos vão se misturar.
- **Campos:** `nome:Tipo`, com `String`, `Integer`, `Long`, `Double`, `Boolean`, `BigDecimal`,
  `LocalDate`, `LocalDateTime`, `Instant` ou `UUID`. Texto é validado com `@NotBlank`, e o resto
  com `@NotNull`.
- **Nunca sobrescreve.** Se algum arquivo já existe, nada é gravado. Arquivos comuns a todas as
  entidades, como o `@RestControllerAdvice`, só são criados se o projeto ainda não tem um.
- **Erros no padrão.** 404 e erros de validação respondem com ProblemDetail (RFC 9457), dizendo
  quais campos falharam.
- **Quarkus.** Num projeto Quarkus (uma dependência `io.quarkus` no build), as mesmas três
  arquiteturas saem do jeito do Quarkus: resource Jakarta REST, `PanacheRepository`, `@Inject` em
  campo package-private e um `@ServerExceptionMapper` para os 404. Um projeto do code.quarkus.io
  sem código ainda não tem pacote, então o primeiro `crud` pede `--pacote`.
- **Java 17 e `jakarta`.** O código gerado é do Spring Boot 3 e do Quarkus 3. Num projeto abaixo do
  Java 17, que ainda importa `javax.persistence` e companhia, ou cujo build fixa o Spring Boot 2, o
  `crud` recusa e diz o que migrar.

![Joseph Joestar, de JoJo's Bizarre Adventure, correndo enquanto alguém grita "Run away!"](docs/anime/never-overwrites.gif)

*O arquivo já existe? Nigerundayo! O Shishō recua sem gravar nada.*

### Antes de gerar: por quê?

O hexagonal custa mais arquivos que um CRUD costuma precisar. Antes de gerá-lo, o Shishō
pergunta o motivo, e sem motivo não grava nada:

```
$ shisho crud --arquitetura hexagonal ./loja Produto nome:String preco:BigDecimal
⚠ Este CRUD em Hexagonal (Ports & Adapters) cria 12 arquivos, e nada no contexto do shisho.yaml paga
esse custo. A arquitetura vale a pena quando o projeto tem: multiplas_integracoes_externas,
dominio_com_regras_complexas, troca_provavel_de_infra, vida_util_longa. Para um CRUD sem regras de
negócio, package-by-feature faz o mesmo com menos arquivos.

Nada foi gravado. Se o projeto tem um desses motivos, declare-o em contexto.caracteristicas no
shisho.yaml: a decisão fica registrada, e o shisho analyze passa a usá-la. Para gerar mesmo assim,
use --mesmo-assim.
```

![Gintoki, de Gintama, segurando uma caixinha de leite de morango com cara de descrença](docs/anime/cost-warning.gif)

*Hexagonal. Para um CRUD de dois campos. Sério?*

Com o motivo declarado no [`shisho.yaml`](#-overengineering), o CRUD sai direto, e o relatório
diz qual motivo pagou o custo. O `--mesmo-assim` serve para quem só quer ver como a arquitetura
fica. Num projeto que já é hexagonal, o próximo `crud` segue sem perguntar: a decisão já foi
tomada. Por que assim: [ADR 0006](docs/adr/0006-crud-cost-warning.md).

Logo depois de gerado, o `analyze` dá **"overengineering, por enquanto"** a esse CRUD: o domínio
ainda é uma cópia da tabela, com os mesmos campos da entidade JPA. O veredito muda quando o
domínio ganha regras próprias. Não há template clean: para um CRUD, ele só acrescentaria camadas
ao que o hexagonal já mostra ([ADR 0011](docs/adr/0011-no-clean-crud.md)).

![Dazai, de Bungou Stray Dogs, ajoelhado no jardim e rindo à toa](docs/anime/cost-paid.gif)

*Com o motivo declarado no `shisho.yaml`, o Dazai sai do tambor.*

Um teste aplica cada template a um projeto igual ao do Initializr e exige que o `shisho analyze`
reconheça a arquitetura e não encontre **nenhuma** lição. O código foi conferido também num
projeto real do Initializr (Spring Boot 4.1) e num do code.quarkus.io (Quarkus 3.40): compila, sobe
e responde. Os templates ficam em [`catalog/scaffolds`](catalog/scaffolds), em Markdown. Por que
assim: [ADR 0005](docs/adr/0005-crud-scaffolding.md) e, para o Quarkus,
[ADR 0014](docs/adr/0014-quarkus-crud.md).

## 🧠 O LLM explica, não julga

Tudo que o Shishō decide é determinístico: mesma entrada, mesmo resultado, pronto para CI. O
LLM local (via [Ollama](https://ollama.com)) é **opcional e vem desligado**: só o `shisho ask`
fala com ele. O modelo recebe os fatos e o veredito já calculados, mais o texto do catálogo
sobre a arquitetura detectada e a sugerida, e só escreve a explicação:

```
$ shisho ask ./loja-api "por que não Clean Architecture aqui?"
Perguntando ao gemma3:4b em http://localhost:11434...
Aluno, a análise indica que o projeto apresenta um overengineering significativo, utilizando a
Clean Architecture de forma excessiva. [...]

Observe que o mapper converte `UsuarioJpaEntity` em `Usuario` com exatamente os mesmos campos.
[...] Os quatro use cases para operações CRUD de `Usuario` também são um exemplo de excesso, com
um use case por método, sem necessidade de regras de negócio que justifiquem essa separação.
[...]

Explicação escrita por gemma3:4b, que pode errar. O veredito, a graduação e as lições vêm da
análise determinística: shisho analyze.
```

Sem pergunta, ele explica o veredito, a graduação e por onde começar. Para usar:

```bash
# instale o Ollama: https://ollama.com/download
ollama pull gemma3:4b                                  # o modelo padrão, ~3 GB
java -jar target/shisho.jar ask ~/projetos/minha-api   # explica o veredito
java -jar target/shisho.jar ask --model qwen2.5:7b ~/projetos/minha-api "qual lição corrijo primeiro?"
```

O `gemma3:4b` é leve, mas às vezes troca uma severidade ou um detalhe; modelos maiores (7B a
14B) tendem a se ater melhor aos fatos. `--url` aponta para um Ollama em outra máquina. Por
que Ollama, sem SDK, e como o prompt é montado: [ADR 0004](docs/adr/0004-local-llm.md).

![Gintoki, de Gintama, de jaleco escrevendo o próprio nome no quadro enquanto a turma chama "Sensei!"](docs/anime/sensei.gif)

*O Ginpachi-sensei dá a aula. A nota, quem dá é o Java.*

## 🤖 No Claude Code

Com o plugin do Shishō, o Claude Code passa a ser conferido enquanto escreve. Depois de cada
edição num `.java`, o Shishō analisa o projeto e mostra ao Claude as lições daquele arquivo, e ele
corrige antes de seguir. Uma skill ensina o Claude a usar a CLI quando você pergunta sobre
arquitetura, overengineering ou pede um CRUD.

```
/plugin install shisho --marketplace mauricioandrade/shisho
```

Se você instalou com o `install.sh`, o plugin acha o jar sozinho; se não, ele pergunta onde está
o `shisho.jar` ao ser habilitado. Num teste real, o Claude escreveu um `@Autowired` em campo, recebeu a lição GRAVE e trocou
pela injeção por construtor sem ninguém pedir.

![Gojo, Yuji e Nobara, de Jujutsu Kaisen, pulando e batendo as mãos para comemorar](docs/anime/claude-code.gif)

*O Claude escreve, o Shishō aponta, o Claude corrige. Toca aqui.*

O Shishō só aponta. Quando uma lição não se aplica, o plugin pede ao Claude que explique isso a
você, em vez de reescrever o código só para calar a kata. Por que assim:
[ADR 0008](docs/adr/0008-claude-code-plugin.md).

## ☕ Versões do Java

O Shishō roda em **Java 25** e analisa projetos a partir do **Java 8**. Ele lê a versão do seu
projeto e **só cobra o que ela permite**. O que só existe numa versão mais nova vira sugestão,
sem tirar nota:

```
i INFO   [prefer-scoped-values]  NotificacaoService.java:11
         Ao migrar para o Java 25: O campo USUARIO_ATUAL é um ThreadLocal. ...
```

Do mesmo jeito, num projeto 17, trocar o pool de threads por virtual threads aparece como o que o
21 traria, e num projeto 11 o DTO que devia ser record aparece como o que o 16 traria. Se o build não declara versão (herdada de um parent remoto, por exemplo), o Shishō
assume 21 e avisa.

Projetos Spring Boot 2 (e Quarkus 2) são reconhecidos pela versão que o build fixa (o `<parent>`,
o plugin do Gradle) ou, sem ela, pelos imports `javax.*`: o `shisho fix`
importa `javax.validation.Valid`, e nenhuma lição recomenda o que só o Boot 3 tem
([ADR 0018](docs/adr/0018-java-8-and-javax.md)).

Recursos em preview nunca são recomendados. O que o Shishō sabe sobre cada versão fica em
[`catalog/java-features`](catalog/java-features).

![Goku Super Saiyajin, de Dragon Ball Z, virando para a câmera com um sorriso e fazendo joinha](docs/anime/java-versions.gif)

*Subiu o projeto para o Java 25? O Shishō desbloqueia as katas novas.*

## 📊 Relatórios e códigos de saída

Além do terminal, o relatório sai em SARIF (para o code scanning do GitHub e outras ferramentas)
ou em HTML (uma página só, sem dependências, com tema claro e escuro):

```bash
shisho analyze --format sarif --output shisho.sarif ~/projetos/minha-api
shisho analyze --format html  --output shisho.html  ~/projetos/minha-api
```

O código de saída foi pensado para CI e não depende do formato:

| Código | Significado |
|---|---|
| 0 | nenhuma lição grave |
| 1 | há pelo menos uma lição **GRAVE** (com baseline, uma lição grave nova) |
| 2 | entrada inválida (caminho inexistente, argumento faltando, `--output` que não pode ser gravado, `SHISHO_IDIOMA` desconhecido; no `crud`, uma arquitetura que nada no contexto paga) |
| 3 | o projeto não pôde ser analisado (ex.: Java abaixo do 8, `shisho.yaml` inválido); no `fix --apply`, um arquivo que não pôde ser gravado; no `ask`, o Ollama fora do ar ou o modelo não instalado |

## 🚦 No GitHub Actions

```yaml
jobs:
  shisho:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      security-events: write   # para enviar o SARIF ao code scanning
    steps:
      - uses: actions/checkout@v4
      - uses: mauricioandrade/shisho@main
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

### Num projeto que já existe: a baseline

Num projeto antigo, a primeira análise costuma trazer dezenas de lições, e uma só grave já reprova
o job. Para adotar o Shishō sem consertar tudo antes, grave as lições de hoje e versione o arquivo:

```bash
shisho baseline .
git add shisho-baseline.yaml
```

Daí em diante, o `analyze` (e a action, e o hook do Claude Code) mostra só as lições **novas**, e o
código de saída 1 só vem de uma lição grave nova. As conhecidas continuam contando na graduação, ficam
recolhidas no relatório HTML e vão para o SARIF marcadas como `unchanged`. O `fix` continua corrigindo
todas. Quando alguém corrige uma lição da baseline, o relatório avisa; rode `shisho baseline` de novo
para a dívida só diminuir.

Uma lição é reconhecida pela kata, pelo arquivo e pelo que ela aponta (o campo, o tipo), nunca pela
linha: mover o código dentro do arquivo não a torna nova. Renomear o arquivo torna. Ver o
[ADR 0023](docs/adr/0023-baseline.md).

![Naoya Zen'in, de Jujutsu Kaisen, de olhos fechados e impassível, enquanto Choso grita atrás dele](docs/anime/github-actions.gif)

*Alguém gritando no canal porque o build quebrou. O Shishō, calmo, já tinha avisado no PR.*

## 🧰 Desenvolvimento

Para mexer no código do Shishō, você precisa de **JDK 25** e **Maven 3.9**. Se já tem os dois,
pule direto para o `mvn verify`.

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

O `target/shisho.jar` é a versão que você acabou de compilar: `java -jar target/shisho.jar
analyze <projeto>`. Antes de abrir um PR, rode o `mvn verify` e o Shishō nele mesmo (`java -jar
target/shisho.jar analyze .`): o código do Shishō precisa passar nas próprias katas.

Para usar a versão compilada no lugar da instalada, rode `./install.sh --from-source` no clone.

O mise é usado só para conveniência de desenvolvimento. Ele não é dependência do Shishō, e
nada no build exige que você o use.

**A demo do README** é gravada pelo [`docs/demo/record.sh`](docs/demo/record.sh), com o
[VHS](https://github.com/charmbracelet/vhs) num contêiner Docker, nas duas línguas. Mudou a saída
do Shishō? Rode o script de novo. Um teste garante que o projeto da demo continua contando a
mesma história.

**Publicar uma versão** é escrever a seção dela no [`CHANGELOG.md`](CHANGELOG.md) e no
[`CHANGELOG.en.md`](CHANGELOG.en.md) e criar a tag: `git tag v0.2.0 && git push origin v0.2.0`. O
workflow [`release.yml`](.github/workflows/release.yml) compila com essa versão, roda todos os
testes e publica o `shisho.jar` e o checksum numa GitHub Release, com a seção do changelog como
notas, e o `install.sh` passa a baixá-la. Por
que assim: [ADR 0010](docs/adr/0010-release.md).

## 📍 Roadmap

- [x] **Fase 1** — CLI, catálogo, 7 katas de AST, relatório no terminal
- [x] **Fase 2** — katas de projeto (JPA, transações, tratamento de erros, build,
  configuração) e `shisho.yaml`
- [x] **Fase 3** — classificação de arquitetura, regras de dependência e veredito de
  overengineering/subengenharia
- [x] **Fase 4** — relatório HTML, SARIF e GitHub Action
- [x] **Fase 5** — modo sensei (exemplos antes/depois), catálogo de recursos do Java,
  `shisho fix`
- [x] **Fase 6** — LLM local opcional (`shisho ask`)
- [x] **Fase 7** — gerador de CRUD (`shisho crud`): layered, package-by-feature e hexagonal,
  com aviso de overengineering antes de gerar
- [x] **Fase 8** — português e inglês, tanto nos relatórios quanto no código analisado
  (`SHISHO_IDIOMA`, `install.sh`)
- [x] **Fase 9** — plugin para o Claude Code: o Shishō confere cada arquivo que o Claude edita
- [x] **Fase 10** — modo shoshin (`--shoshin`): os conceitos de Java, Spring e padrões de projeto
  que aparecem no código
- [x] **Fase 11** — release com o jar pronto a cada tag, e instalação com um comando
- [x] **Fase 12** — Quarkus: as katas, o shoshin e a classificação reconhecem CDI, Jakarta REST
  e Panache
- [x] **Fase 13** — Gradle: `build.gradle`, `build.gradle.kts` e os version catalogs (bibliotecas e bundles) lidos sem executar o script
  ([ADR 0013](docs/adr/0013-gradle.md))
- [x] **Fase 14** — `shisho crud` em projetos Quarkus, nas três arquiteturas
  ([ADR 0014](docs/adr/0014-quarkus-crud.md))
- [x] **Fase 15** — multi-módulo: os módulos do Maven (de profiles também) e do Gradle e os convention plugins do `buildSrc`, cada lição no build que a declara
  ([ADR 0015](docs/adr/0015-multi-module.md))
- [x] **Fase 16** — calibração com projetos reais (petclinic, JHipster, buckpal, quickstarts do
  Quarkus...): seis falsos positivos e um falso negativo corrigidos ([ADR 0016](docs/adr/0016-real-projects.md))
- [x] **Fase 17** — projetos a partir do Java 8 e do Spring Boot 2: o que precisa de uma versão
  mais nova vira sugestão de migração ([ADR 0017](docs/adr/0017-java-17.md),
  [ADR 0018](docs/adr/0018-java-8-and-javax.md))
- [x] **Fase 18** — katas novas: `unproxyable-method`, `swallowed-exception` e
  `entity-with-lombok-data` ([ADR 0019](docs/adr/0019-new-katas.md))
- [x] **Fase 19** — auto-invocação: `this.salvar()` que escapa do proxy ([ADR 0020](docs/adr/0020-self-invocation.md))
- [x] **Fase 20** — katas de JPA: fetch, construtor da entidade e transação no controller ([ADR 0021](docs/adr/0021-jpa-katas.md))
- [x] **Fase 21** — desempenho: parse em paralelo e a análise 2× mais rápida em projetos grandes ([ADR 0022](docs/adr/0022-performance.md))
- [x] **Fase 22** — baseline: adotar o Shishō num projeto existente e reprovar só as lições novas ([ADR 0023](docs/adr/0023-baseline.md))
- [x] **Fase 23** — katas de configuração: o `application.properties`/`.yml` com perfis; schema apagado, segredo escrito, open-in-view e SQL no log ([ADR 0024](docs/adr/0024-settings-katas.md))
- [ ] **Depois** — outras stacks, reaproveitando o catálogo de arquiteturas

![Sung Jinwoo, de Solo Leveling, atacando com uma adaga em meio a estátuas de pedra](docs/anime/roadmap.gif)

*Uma fase de cada vez, como quem limpa uma dungeon.*

## 🤝 Contribuindo

O jeito mais fácil de contribuir não exige escrever Java: **o catálogo é Markdown**. Uma
arquitetura nova, um sinal de overengineering que faltou, uma explicação mais clara, um template
de CRUD para outra arquitetura — tudo é um PR em `catalog/`. O schema está em [`catalog/README.md`](catalog/README.md).
Cada texto tem uma versão em português e outra em inglês (`catalog/en/`). Se você só escreve num
dos dois, abra o PR assim mesmo: o teste mostra o que falta, e alguém completa.

![Naruto fazendo o selo de mão do Kage Bunshin no Jutsu](docs/anime/contributing.gif)

*Um dev sozinho não cobre todas as katas. Precisamos de clones.*

Issues marcadas com `good first issue` são um bom ponto de partida.

## 📄 Licença

Código sob [Apache 2.0](LICENSE). Os GIFs de anime pertencem aos seus criadores e estúdios
e são usados apenas como ilustração.

## 🙏 Créditos

- [mise](https://github.com/jdx/mise), de Jeff Dickey e contribuidores (MIT) — gerenciamento
  das versões de Java e Maven no desenvolvimento.
- [JavaParser](https://github.com/javaparser/javaparser) (Apache 2.0 / LGPL) — parser de
  código Java usado pelas katas. A escolha está em [`docs/adr/0001-parser.md`](docs/adr/0001-parser.md).
- [picocli](https://picocli.info) (Apache 2.0) — linha de comando.
- [SnakeYAML](https://bitbucket.org/snakeyaml/snakeyaml) (Apache 2.0) — leitura do frontmatter
  do catálogo e das respostas do Ollama.
- [Ollama](https://ollama.com) (MIT) — servidor do LLM local opcional; não vai dentro do jar.

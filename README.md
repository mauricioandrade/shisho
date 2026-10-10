<div align="center">

# 師匠 Shishō

**O mestre que lê seu código, aponta o que está errado e explica por quê.**

Começando por Java e Spring Boot.

**Português** · [English](README.en.md)

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
  com explicação, em português ou em inglês.
- 🏯 **Classificação de arquitetura** — reconhece se o projeto é layered, package-by-feature,
  monolito modular, hexagonal, clean, CQRS, event-driven ou microsserviços.
- ⚖️ **Veredito de overengineering** — cruza o que está no código com o contexto do projeto e
  diz se a arquitetura está grande demais (ou pequena demais) para o problema.
- 🎖️ **Graduação** — do 6º kyū ao 1º dan.
- 🧠 **LLM local opcional** — um modelo rodando na sua máquina explica o veredito. Ele explica,
  nunca decide.
- 🏗️ **Gerador de CRUD** — cria um CRUD REST na arquitetura do projeto, explicando o papel de
  cada arquivo. O código gerado passa em todas as katas.
- 🤖 **Plugin para o Claude Code** — confere cada arquivo que o Claude edita e mostra a ele as
  lições, enquanto ele escreve.

## Por que existe

- **Linters apontam, não ensinam.** Um warning diz que algo está errado, mas quem está
  aprendendo precisa do porquê, do custo e de quando a regra não vale. O Shishō escreve a lição
  inteira.
- **Arquitetura demais também é erro.** Muito tutorial ensina hexagonal ou Clean Architecture
  para um CRUD de três campos, e nenhuma ferramenta pergunta se a arquitetura cabe no projeto. O
  Shishō pergunta, e responde com fatos medidos no código.
- **Determinístico e local.** Mesma entrada, mesmo resultado: dá para rodar em CI. Seu código não
  sai da máquina, nem quando você usa o LLM, que roda localmente e é opcional.

## Instalação

Ainda não há release com o jar pronto: o Shishō é compilado a partir do código. Você precisa de
**JDK 25** e **Maven 3.9** (ou do [mise](#rodando-a-partir-do-código), que instala os dois só para
esta pasta).

```bash
git clone https://github.com/mauricioandrade/shiso.git
cd shiso
./install.sh        # compila, pergunta o idioma e adiciona o comando shisho ao seu shell
```

O instalador pergunta `pt` ou `en` e, se você deixar, adiciona o alias `shisho` e a variável
`SHISHO_IDIOMA` ao seu `~/.bashrc` ou `~/.zshrc`; rodar de novo substitui os dois. Em português
ou em inglês, o idioma vale para tudo: relatórios, guias do modo sensei e o código que o
`shisho crud` gera. Para trocar só num comando: `SHISHO_IDIOMA=en shisho analyze .`. Prefere
fazer à mão? `mvn verify` gera o `target/shisho.jar`, e
`alias shisho="java -jar $PWD/target/shisho.jar"` faz o resto.

O código analisado pode estar em português ou em inglês: o Shishō reconhece pacotes como
`dominio`, `servico` e `porta/entrada` tanto quanto `domain`, `service` e `port/in`.

## Uso rápido

Aponte para a raiz de um projeto Maven com Java 21 ou superior:

| Comando | O que faz |
|---|---|
| `shisho analyze ./meu-projeto` | lições, arquitetura, veredito e graduação |
| `shisho analyze --sensei ./meu-projeto` | o mesmo, com o porquê e o antes/depois de cada lição |
| `shisho analyze --format html --output r.html ./meu-projeto` | relatório HTML (ou `sarif`, para o GitHub) |
| `shisho fix ./meu-projeto` | mostra as correções automáticas; `--apply` grava |
| `shisho crud --arquitetura layered ./meu-projeto Produto nome:String` | cria um CRUD que explica cada arquivo |
| `shisho ask ./meu-projeto "por onde começo?"` | um LLM local explica o veredito (precisa do Ollama) |
| `/plugin install shisho --marketplace mauricioandrade/shiso` | no Claude Code: o Shishō confere cada arquivo que o Claude edita |

`shisho <comando> --help` lista todas as opções.

## Demo

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

| Kata | Severidade | O que aponta | Por que importa |
|---|---|---|---|
| `field-injection` | GRAVE | `@Autowired`/`@Inject` em campo | `new` cria o objeto incompleto, e o teste precisa de Spring |
| `lombok-constructor` | MEDIA | `@RequiredArgsConstructor`/`@AllArgsConstructor` | as dependências somem do código, e a décima entra sem ninguém notar |
| `non-final-dependency` | MEDIA | dependência injetada sem `final` | qualquer método pode trocá-la depois da construção |
| `system-out` | MEDIA | `System.out`, `System.err`, `printStackTrace()` | sem nível nem horário, e fora do agregador de logs |
| `request-body-without-valid` | MEDIA | `@RequestBody` sem `@Valid` | a validação do DTO é ignorada, e dado inválido chega ao service |
| `entity-in-controller` | MEDIA | endpoint que recebe ou devolve uma `@Entity` JPA | o banco vira o contrato da API, e campos internos vazam |
| `missing-controller-advice` | MEDIA | controllers sem nenhum `@RestControllerAdvice` | cada endpoint trata (ou esquece) os próprios erros |
| `avoided-dependency` | MEDIA | dependência abandonada, substituída ou movida (Log4j 1, Springfox, Joda-Time... ver [`catalog/dependencies`](catalog/dependencies)) | sem correções de segurança, ou já substituída pelo próprio projeto |
| `value-outside-constructor` | LEVE | `@Value` fora de parâmetro de construtor | o valor chega por reflexão, e um teste com `new` fica sem ele |
| `dto-not-record` | LEVE | DTO (por pacote ou sufixo) que não é record | getters, `equals` e `hashCode` escritos à mão, e mutável |
| `transactional-read-only` | LEVE | método de leitura (`find…`, `buscar…`) numa transação de escrita | o Hibernate faz dirty checking à toa |
| `configuration-properties-not-record` | LEVE | `@ConfigurationProperties` numa classe com setters | a configuração pode ser alterada depois de carregada |
| `prefer-configuration-properties` | LEVE | 3 ou mais `@Value` com o mesmo prefixo na mesma classe | propriedades soltas, sem tipo nem validação num lugar só |
| `missing-actuator` | LEVE | aplicação web sem `spring-boot-starter-actuator` | sem health check, o orquestrador não sabe se a aplicação está viva |
| `missing-springdoc` | INFO | aplicação web sem springdoc-openapi | quem consome a API não tem contrato nem Swagger UI |
| `prefer-virtual-threads` | INFO | `Executors.newFixedThreadPool`/`newCachedThreadPool` (Java 21) | para I/O, virtual threads dispensam dimensionar pool |
| `prefer-scoped-values` | INFO | campo `ThreadLocal` (Java 25) | `ScopedValue` não vaza entre tarefas e não precisa de `remove()` |

As sugestões (INFO) não tiram nota. Com `--sensei`, cada lição vem com a explicação completa, os
casos em que o código acusado é aceitável e o exemplo antes/depois.

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

## Versões do Java

O Shishō roda em **Java 25** e analisa projetos a partir do **Java 21**. Ele lê a versão do seu
projeto e **só cobra o que ela permite**. O que só existe numa versão mais nova vira sugestão,
sem tirar nota:

```
i INFO   [prefer-scoped-values]  NotificacaoService.java:11
         Ao migrar para o Java 25: O campo USUARIO_ATUAL é um ThreadLocal. ...
```

Recursos em preview nunca são recomendados. O que o Shishō sabe sobre cada versão fica em
[`catalog/java-features`](catalog/java-features).

![Goku Super Saiyajin, de Dragon Ball Z, virando para a câmera com um sorriso e fazendo joinha](docs/anime/java-versions.gif)

*Subiu o projeto para o Java 25? O Shishō desbloqueia as katas novas.*

## Roadmap

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
- [ ] **Depois** — template de CRUD para clean; outras stacks, reaproveitando o catálogo de
  arquiteturas
- [ ] **Ideias** — modo `--shoshin` (初心, "mente de principiante"), que explica cada conceito
  que aparece no código (o que é um record, uma sealed interface…) e os design patterns

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
| 2 | entrada inválida (caminho inexistente, argumento faltando, `--output` que não pode ser gravado, `SHISHO_IDIOMA` desconhecido; no `crud`, uma arquitetura que nada no contexto paga) |
| 3 | o projeto não pôde ser analisado (ex.: Java abaixo do 21, `shisho.yaml` inválido); no `fix --apply`, um arquivo que não pôde ser gravado; no `ask`, o Ollama fora do ar ou o modelo não instalado |

O mise é usado só para conveniência de desenvolvimento. Ele não é dependência do Shishō, e
nada no build exige que você o use.

## Modo sensei: aprender e corrigir

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

Só as linhas da correção mudam: formatação e comentários ficam como estavam. Quando a correção
não é segura (um `@Qualifier` que teria de mudar de lugar, um `@Builder` que depende do
construtor do Lombok…), a lição fica para você, com o exemplo do modo sensei. Por quê
JavaParser e não OpenRewrite: [ADR 0003](docs/adr/0003-autofix.md).

## Do zero: `shisho crud`

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

Com o motivo declarado no [`shisho.yaml`](#overengineering), o CRUD sai direto, e o relatório
diz qual motivo pagou o custo. O `--mesmo-assim` serve para quem só quer ver como a arquitetura
fica. Num projeto que já é hexagonal, o próximo `crud` segue sem perguntar: a decisão já foi
tomada. Por que assim: [ADR 0006](docs/adr/0006-crud-cost-warning.md).

![Dazai, de Bungou Stray Dogs, ajoelhado no jardim e rindo à toa](docs/anime/cost-paid.gif)

*Com o motivo declarado no `shisho.yaml`, o Dazai sai do tambor.*

Um teste aplica cada template a um projeto igual ao do Initializr e exige que o `shisho analyze`
reconheça a arquitetura e não encontre **nenhuma** lição. O código foi conferido também num
projeto real do Initializr (Spring Boot 4.1): compila, sobe e responde. Os templates ficam em
[`catalog/scaffolds`](catalog/scaffolds), em Markdown. Por que assim: [ADR 0005](docs/adr/0005-crud-scaffolding.md).

## No Claude Code

Com o plugin do Shishō, o Claude Code passa a ser conferido enquanto escreve. Depois de cada
edição num `.java`, o Shishō analisa o projeto e mostra ao Claude as lições daquele arquivo, e ele
corrige antes de seguir. Uma skill ensina o Claude a usar a CLI quando você pergunta sobre
arquitetura, overengineering ou pede um CRUD.

```
/plugin install shisho --marketplace mauricioandrade/shiso
```

Ao habilitar o plugin, o Claude Code pede o caminho do `shisho.jar`, que o `install.sh` mostra no
fim. Num teste real, o Claude escreveu um `@Autowired` em campo, recebeu a lição GRAVE e trocou
pela injeção por construtor sem ninguém pedir.

O Shishō só aponta. Quando uma lição não se aplica, o plugin pede ao Claude que explique isso a
você, em vez de reescrever o código só para calar a kata. Por que assim:
[ADR 0008](docs/adr/0008-claude-code-plugin.md).

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
arquitetura nova, um sinal de overengineering que faltou, uma explicação mais clara, um template
de CRUD para outra arquitetura — tudo é um PR em `catalog/`. O schema está em [`catalog/README.md`](catalog/README.md).
Cada texto tem uma versão em português e outra em inglês (`catalog/en/`). Se você só escreve num
dos dois, abra o PR assim mesmo: o teste mostra o que falta, e alguém completa.

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
  do catálogo e das respostas do Ollama.
- [Ollama](https://ollama.com) (MIT) — servidor do LLM local opcional; não vai dentro do jar.

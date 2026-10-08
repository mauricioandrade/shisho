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

Arquitetura detectada: Camadas técnicas (82%)

✗ GRAVE  [field-injection]          UsuarioController.java:14
         Injeção em campo. Use construtor explícito com private final:
         a dependência fica visível e o objeto, imutável.
✗ GRAVE  [jpa-entity-exposed]       PedidoController.java:31
         O endpoint retorna a entidade JPA Pedido. Crie um record PedidoResponse.
! MEDIA  [request-body-without-valid] PedidoController.java:27
         @RequestBody sem @Valid: as anotações do DTO nunca são verificadas.

Graduação: 3º kyū  (12 lições: 2 graves, 6 médias, 4 leves)
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

## Overengineering

A pergunta que o Shishō mais gosta de responder: **essa arquitetura cabe nesse projeto?**

Overengineering não é uma propriedade do código sozinho, é a relação entre a complexidade da
solução e a do problema. Por isso o Shishō junta duas coisas: os **fatos medidos** no código
(classes por feature, interfaces com uma implementação só, use cases que só repassam chamadas,
microsserviços dividindo banco…) e o **contexto** que você declara:

```yaml
# shisho.yaml
contexto:
  tipo: portfolio        # mvp | portfolio | produto | corporativo
  time: 1
  vida_util_esperada: curta
  carga_esperada: baixa
```

E aí vem o veredito:

> Clean Architecture detectada. Cerca de 11 classes por feature para 3 entidades sem regra de
> domínio, time de 1 pessoa. Considere **package-by-feature**.

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

- [ ] **Fase 1** — CLI, catálogo, 7 katas de AST, relatório no terminal
- [ ] **Fase 2** — katas com resolução de tipos e de projeto, `shisho.yaml`
- [ ] **Fase 3** — classificação de arquitetura e veredito de overengineering
- [ ] **Fase 4** — relatório HTML, SARIF e GitHub Action
- [ ] **Fase 5** — exemplos antes/depois, catálogo de recursos do Java, autofix com OpenRewrite
- [ ] **Fase 6** — LLM local opcional
- [ ] **Depois** — outras stacks, reaproveitando o catálogo de arquiteturas

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

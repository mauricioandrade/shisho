# ADR 0009 — Modo shoshin: explicar os conceitos que aparecem no código

- **Status:** aceito
- **Data:** 2026-10-09
- **Fase:** 10 (modo shoshin)

## Contexto

O modo sensei explica as **lições**: o que está errado e por quê. Quem está começando também
precisa do contrário: entender o que está **certo**, ou pelo menos o que já está ali. Um projeto
recém-gerado pelo `shisho crud` tem record, `@Transactional`, injeção por construtor e um
repositório do Spring Data, e nada disso vira lição, porque está tudo no lugar. Quem nunca viu
esses conceitos não aprende nada com um relatório que só diz "nenhuma lição".

Shoshin (初心) é a "mente de principiante" do zen: olhar para o conhecido como se fosse a
primeira vez. Foi a ideia do "/noob" que abriu a Fase 7.

## Decisão

### 1. `shisho analyze --shoshin`

Uma opção do `analyze`, como o `--sensei`, e as duas combinam. Depois das lições, o relatório
lista os **conceitos** encontrados no projeto, em três grupos: Java, Spring e padrões de projeto.
Cada conceito traz onde aparece (até três lugares e o total) e uma explicação curta do que é e do
que ele te dá.

### 2. Os conceitos vivem no catálogo: `catalog/concepts/`

Um arquivo por conceito, com o mesmo formato do resto do catálogo, e a tradução em
`catalog/en/concepts/`. O frontmatter diz **como reconhecer** o conceito (`aparece_em`); o corpo
tem `## O que é` (a explicação mostrada no terminal) e `## Exemplo` (um trecho de código).

`aparece_em` é uma lista de sinais, e basta um deles. Cada sinal tem uma ou mais condições sobre
a **mesma** classe, e todas precisam valer:

| Condição | Exemplo | Casa com |
|---|---|---|
| `anotacao` | `Transactional` | a anotação na classe, num campo, método, construtor ou parâmetro |
| `tipo` | `record` | o tipo da declaração: `class`, `interface`, `enum`, `record` |
| `modificador` | `sealed` | um modificador da declaração |
| `import` | `java.util.Optional` | um import com esse prefixo |
| `estende` | `JpaRepository` | um supertipo, pelo nome simples |
| `chamada` | `stream` | uma chamada de método com esse nome |
| `sufixo` | `Request` | o nome da classe termina assim |
| `detector` | `estrategia` | um detector em Java, para o que o frontmatter não descreve |

Os detectores ficam para os padrões que dependem de mais de uma classe: um **Strategy** é uma
interface do projeto com duas ou mais implementações. Um ID de detector desconhecido é ignorado,
como nos sinais de arquitetura: o catálogo pode andar na frente do código.

### 3. O exemplo de cada conceito é testado

Como os guias das katas, cada conceito tem um `## Exemplo` em Java, e um teste exige que os
sinais do conceito reconheçam o próprio exemplo, nos dois idiomas. Um sinal que não reconhece
nem o exemplo do catálogo não reconhece o código de ninguém.

### 4. Reconhecer, não julgar

O shoshin não dá nota nem muda a graduação: ele descreve. Os conceitos ficam no `Analysis`
(`concepts`), ao lado das lições, e só o relatório decide se os mostra. Um padrão reconhecido não
é um elogio: um Strategy com duas implementações pode ser exatamente o certo ou overengineering,
e quem diz isso é o veredito.

## Alternativas registradas

- **Um comando `shisho shoshin`.** Repetiria a análise inteira para mostrar a outra metade do
  mesmo relatório. Como opção, combina com `--sensei` e com os outros formatos depois.
- **Regras de reconhecimento em Java, uma classe por conceito.** Tiraria o conhecimento do
  catálogo. Só os padrões que olham várias classes precisam de Java.
- **Explicar com o LLM.** O texto de cada conceito é revisado e versionado; o modelo poderia
  inventar.
- **Mostrar todos os conceitos do catálogo.** O valor está em partir do código da pessoa: "isto
  aqui no seu projeto é um record".

## Consequências

- `analyze --shoshin` no terminal. O HTML e o SARIF não mudam nesta fase.
- O `ClassUnit` passa a guardar supertipos e modificadores, que o reconhecimento precisa.

## 📚 Estudo — aprender pelo próprio código

Ler a teoria de um padrão de projeto e depois procurá-lo no código é o caminho comum, e o mais
difícil: o exemplo do livro nunca se parece com o seu código. O shoshin faz o caminho inverso:
parte de uma linha que você já tem aberta no editor e dá nome ao que está ali. O nome é o que
permite pesquisar, perguntar e reconhecer o mesmo padrão no próximo projeto.

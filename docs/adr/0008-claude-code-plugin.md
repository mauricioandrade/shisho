# ADR 0008 — Plugin para o Claude Code: o Shishō confere enquanto o Claude escreve

- **Status:** aceito
- **Data:** 2026-10-09
- **Fase:** 9 (plugin para o Claude Code)

## Contexto

Até aqui o Shishō rodava depois: alguém escrevia o código e só então chamava o `analyze`. Com um
assistente como o Claude Code escrevendo boa parte do código, dá para o mestre olhar **durante**:
o Claude edita um arquivo, o Shishō aponta o que ficou fora das katas e o Claude corrige antes de
seguir.

Isso combina com o princípio central do projeto: o Shishō julga de forma determinística, e o
modelo explica e age. Aqui o modelo é o Claude, e o julgamento continua sendo do Java.

## Decisão

### 1. Um plugin do Claude Code, no próprio repositório

O repositório passa a ser também um marketplace de plugins: `.claude-plugin/marketplace.json` na
raiz aponta para `claude-plugin/`. Quem usa instala com:

```
/plugin marketplace add mauricioandrade/shisho
/plugin install shisho@shisho
```

O plugin tem duas peças:

- **Um hook `PostToolUse`** para `Edit` e `Write`. Depois de cada edição, ele roda o Shishō no
  projeto do arquivo editado e devolve ao Claude as lições **daquele arquivo**. No
  `PostToolUse`, sair com código 2 mostra o stderr ao Claude; sem lição, o hook sai com 0 e não
  diz nada.
- **Uma skill `shisho`**, que ensina o Claude a usar a CLI quando o pedido é sobre arquitetura,
  overengineering, revisão ou um CRUD novo: `analyze`, `--sensei`, `fix` e `crud`, cada um no seu
  caso.

### 2. O hook é um subcomando do Shishō: `shisho claude-hook`

O Claude Code entrega ao hook um JSON na entrada padrão (`tool_input.file_path`, `cwd`). Ler
isso em shell pediria o `jq`, que nem toda máquina tem, e o script ficaria sem teste. O Shishō já
lê JSON (é como ele conversa com o Ollama), então o hook é um subcomando oculto da própria CLI,
testado como os outros:

1. lê o caminho do arquivo editado;
2. sobe os diretórios até achar o `pom.xml`, e para em silêncio se não achar: não é um projeto
   que o Shishō analisa;
3. analisa o projeto e fica só com as lições daquele arquivo, sem as sugestões (INFO);
4. imprime as lições no mesmo formato do terminal e sai com 2, ou sai com 0 se não houver lição.

Um projeto que não pode ser analisado (Java antigo, `shisho.yaml` inválido) também sai com 0. O
hook não é lugar para esse erro: o `analyze` o mostra quando alguém pedir.

As lições vêm com uma frase final para o Claude: corrigir o que fizer sentido e, quando uma
lição não se aplicar, dizer isso a quem pediu o código em vez de contornar a kata. Um assistente
que reescreve código só para calar o analisador ensina o hábito errado.

### 3. O jar fica onde o usuário o compilou

Ainda não há release com o jar pronto, então o plugin não o carrega. Ao habilitar o plugin, o
Claude Code pergunta o caminho do `shisho.jar` (`userConfig`), e o `install.sh` mostra esse
caminho no fim. O hook lê o valor da variável que o Claude Code exporta
(`CLAUDE_PLUGIN_OPTION_SHISHO_JAR`), e a skill recebe o caminho no próprio texto. Sem o caminho,
o hook avisa uma vez por edição, como erro de hook, e não bloqueia nada.

## Alternativas registradas

- **Servidor MCP com `analyze` e `crud` como ferramentas.** Deixaria o Claude chamar o Shishō
  quando quisesse, mas é exatamente isso que a skill já faz com a CLI, e o MCP exigiria um
  processo a mais. O que o MCP não faz é o principal: olhar a cada edição sem ninguém pedir.
- **Hook em shell com `jq`.** Uma dependência a mais na máquina de quem usa, e sem teste.
- **Analisar só o arquivo editado.** Mais rápido, mas várias katas olham o projeto inteiro (a
  entidade exposta no controller, as regras de dependência). A análise do projeto todo leva
  menos de um segundo nos projetos de teste. Se pesar num projeto grande, o próximo passo é um
  cache, e não uma análise que erra.
- **Bloquear a edição (`PreToolUse`).** O Shishō aponta, não proíbe: a lição chega depois, e o
  Claude decide com quem pediu o código.

## Consequências

- O subcomando `claude-hook` não aparece na ajuda: ele existe para o plugin.
- O idioma das lições segue o `SHISHO_IDIOMA` do ambiente em que o Claude Code foi aberto.

## 📚 Estudo — hooks e feedback rápido

Um hook é um programa que o Claude Code roda em pontos fixos do trabalho: antes de uma
ferramenta, depois dela, ao fim de um turno. É o mesmo princípio do pre-commit e do linter no
editor: quanto mais perto do momento em que o erro nasce, mais barato é corrigir. Uma lição que
chega segundos depois da edição custa uma linha; a mesma lição num code review, dias depois,
custa um retrabalho.

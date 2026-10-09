# ADR 0004 — LLM local: Ollama por HTTP, sem SDK

- **Status:** aceito
- **Data:** 2026-10-09
- **Fase:** 6 (LLM local)

## Contexto

O Shishō já calcula tudo de forma determinística: lições, arquitetura, veredito e graduação. A
Fase 6 acrescenta um **explicador opcional**: `shisho ask` pergunta a um modelo rodando na máquina
do usuário "por que este veredito?" ou qualquer outra dúvida sobre a análise.

Os princípios do CLAUDE.md §2 limitam o desenho:

1. **O LLM explica, não julga.** Ele recebe fatos prontos e não decide nada.
2. **Desligado por padrão.** `analyze` e `fix` nunca falam com um modelo; o Shishō tem de ser
   100% útil sem ele.
3. **Sem dependências gratuitas.** O jar hoje tem JavaParser, picocli e SnakeYAML.

## Decisão

### 1. Ollama pela API HTTP, com `java.net.http.HttpClient`

O adaptador `OllamaLlmProvider` chama `POST /api/chat` com `stream: true` e imprime cada
fragmento assim que ele chega. Num modelo local, uma resposta leva de alguns segundos a um
minuto, e ver o texto aparecendo é a diferença entre "está funcionando" e "travou".

- **Sem SDK.** A API tem um endpoint e meia dúzia de campos. Um cliente pronto traria mais
  dependências do que o código que ele economiza.
- **JSON de saída:** o `Json` que já escrevia o SARIF, movido para `infrastructure/json`.
- **JSON de entrada:** cada linha do stream é lida pelo **SnakeYAML**, que já está no jar. JSON é,
  na prática, um subconjunto de YAML. A exceção conhecida é o escape `\/`, que o YAML 1.1 não
  aceita. O encoder do Go, usado pelo Ollama, nunca o gera.
- **`num_ctx: 8192`.** O Ollama corta, sem avisar, o **começo** de um prompt maior que a janela
  de contexto, e o começo é justamente a instrução de não inventar nada. O prompt típico do
  Shishō fica em torno de 3 mil tokens, e 8 mil deixam folga para a resposta.
- **`temperature: 0.2`.** A resposta deve se apoiar nos fatos, não ser criativa. Zero absoluto
  faz modelos pequenos repetirem frases.

Modelo padrão: **`gemma3:4b`**. Ele cabe em máquinas sem GPU dedicada e escreve bem em português.
`--model` troca o modelo e `--url` troca o servidor.

### 2. O prompt: fatos, veredito e o Markdown das arquiteturas relevantes

`SenseiPrompt` monta duas mensagens a partir da `Analysis` já calculada:

- **system:** o papel do sensei e as regras. Use só os fatos, não mude o veredito nem a
  graduação, e diga "a análise não mostra isso" quando a pergunta sair do que foi medido.
- **user:** projeto e versão do Java, arquitetura detectada, veredito com as evidências,
  graduação, lições **agrupadas por kata** (contagem e até 3 exemplos, para o prompt não crescer
  com o tamanho do projeto), o Markdown da arquitetura detectada e das que o veredito sugere
  (`alternativa_mais_simples`, `evolui_para`), e por fim a pergunta.

Os textos vêm do mesmo `ReportTexts` dos relatórios. O que o modelo lê é exatamente o que o
`analyze` mostra, nas mesmas palavras.

### 3. Onde fica cada peça

- `domain/port/out/LlmProvider`: recebe as duas mensagens e devolve o texto em fragmentos.
- `domain/exception/LlmException`: servidor fora do ar ou modelo não instalado, com uma mensagem
  que diz o que fazer (`ollama serve`, `ollama pull <modelo>`).
- `infrastructure/llm`: `OllamaLlmProvider` e `SenseiPrompt`.
- `infrastructure/cli/AskCommand`: analisa, monta o prompt e transmite a resposta.

Não há caso de uso novo. Montar o prompt é renderizar a análise, como faz um `ReportWriter`, e
o `AnalyzeCommand` já chama a porta `ReportWriter` direto.

### Códigos de saída

`ask` não é feito para CI: devolve 0 mesmo com lições GRAVE. 2 é entrada inválida, como nos
outros comandos. 3 é projeto não analisado **ou modelo indisponível**: nos dois casos não há o
que mostrar.

## Alternativas registradas

- **API compatível com OpenAI (`/v1/chat/completions`)**, que Ollama, LM Studio e llama.cpp
  servem. Abriria o Shishō a outros servidores locais, mas o endpoint compatível do Ollama não
  aceita `num_ctx` por requisição, e sem isso o prompt pode ser truncado em silêncio. Se outro
  servidor for pedido, ele vira um segundo adaptador da mesma porta.
- **Um leitor de JSON próprio** (~100 linhas) em vez do SnakeYAML. Vale trocar se o Shishō
  passar a ler JSON de alguma fonte que não seja o Ollama.
- **Kata guides no prompt.** Ajudariam em perguntas como "por que injeção em campo é ruim?", mas
  dobrariam o prompt. A mensagem de cada lição já traz o porquê curto, e o `--sensei` traz o
  longo.

## Consequências

- `shisho ask <dir> ["pergunta"]` explica o veredito ou responde à pergunta.
- O teste do adaptador usa o `HttpServer` do JDK como um Ollama falso. Nenhum teste depende de
  um modelo instalado.
- A explicação pode errar; os fatos, não. A saída do `ask` termina lembrando disso e apontando
  para `shisho analyze`.

## 📚 Estudo — RAG sem banco vetorial

Dar a um modelo os documentos certos junto com a pergunta chama-se **RAG** (*retrieval-augmented
generation*). Normalmente a parte de "achar os documentos certos" usa embeddings e um banco
vetorial, porque não se sabe de antemão o que é relevante.

No Shishō, a recuperação é **determinística**: a análise já disse qual é a arquitetura e para
onde o veredito aponta, então os documentos relevantes são exatamente esses arquivos do
catálogo. Não há busca por similaridade nem chance de trazer o documento errado. É o mesmo
princípio do resto do projeto: o Java decide o que é verdade e o que é relevante, e o modelo só
transforma isso em prosa.

Um efeito colateral útil: como o modelo só vê o que o Java mandou, uma resposta errada é fácil
de auditar. Basta comparar com a saída do `analyze`.

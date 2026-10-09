# ADR 0003 — Autofix: JavaParser com impressão que preserva o layout

- **Status:** aceito
- **Data:** 2026-10-08
- **Fase:** 5 (modo sensei)

## Contexto

O roadmap pede autofix "via OpenRewrite": `shisho fix` corrige no código as lições que têm uma
correção mecânica. Dois requisitos não negociáveis:

1. **Diff mínimo.** Só as linhas da correção mudam; formatação, comentários e o resto do arquivo
   ficam como o autor deixou. Um autofix que reformata o arquivo inteiro não é aceito em PR.
2. **Java 25.** O mesmo critério do ADR 0001: o código analisado pode usar `import module`,
   corpo de construtor flexível e `_`.

E duas restrições do projeto: o Shishō é um jar único e Apache 2.0, e cada dependência nova
precisa de justificativa (CLAUDE.md §3).

## Spike

Mesma correção nos dois candidatos (`final` na dependência de `PedidoService`, do fixture
"aluno bagunçado"), mais dois testes de robustez no JavaParser. Temurin 25.0.4, JVM fria.

| Verificação | OpenRewrite 8.90.4 (`rewrite-java` + `rewrite-java-25`) | JavaParser 3.28.2 + `LexicalPreservingPrinter` |
|---|---|---|
| Diff de `final` no campo | 1 linha, idêntico ao do JavaParser | 1 linha |
| Tempo (1 arquivo) | ~1.600 ms | ~360 ms |
| Dependências | **41 jars, 23 MB** (Jackson, JGit, Lombok, Micrometer, ANTLR…) | nenhuma nova: já é o parser do Shishō (1,5 MB) |
| Licença do núcleo | Apache 2.0 | Apache 2.0 |
| Receitas prontas para Spring | `rewrite-spring`, `rewrite-static-analysis`: **Moderne Source Available License** | não há; escrevemos as nossas |
| Fixture Java 25 reimpresso sem mudança | (não testado) | ✅ idêntico byte a byte |
| Correção estrutural (`field-injection`: tirar `@Autowired`, `final`, novo construtor) | (não testado) | ✅ correta; construtor vai para o fim da classe e sobra uma linha só com espaços |

## Decisão

**JavaParser com `LexicalPreservingPrinter`**, atrás de uma porta do domínio (`SourceFixer`).

### Por quê

- **As receitas que tornariam o OpenRewrite valioso não podem vir junto.** O núcleo é Apache
  2.0, mas as receitas de Spring e de análise estática estão sob a Moderne Source Available
  License. Sem elas, escreveríamos as receitas do mesmo jeito, só que num segundo modelo de AST.
- **Custo.** 23 MB e 41 jars a mais para um jar que hoje tem 2,3 MB, por um resultado que, nas
  correções que o Shishō faz, é igual.
- **Um parser só.** As katas já leem a árvore do JavaParser; o fixer reencontra os mesmos nós
  pela linha e pelos argumentos da lição.
- Os dois defeitos vistos na correção estrutural se resolvem no nosso código: inserir o
  construtor depois dos campos e limpar espaços só nas linhas que a correção acrescentou.

### Salvaguardas

- **Só correções mecânicas e seguras.** Uma kata ganha fixer quando a correção não depende de
  julgamento (adicionar `final`, `@Valid`, `readOnly = true`, construtor explícito). Trocar
  `System.out` por logger ou classe por record pede decisões do autor e fica fora.
- **Cada fixer verifica o que não é visível na lição.** Ex.: `final` só entra se o campo não é
  atribuído fora dos construtores.
- **O resultado precisa parsear.** Se o arquivo corrigido não parseia no nível de linguagem do
  projeto, a correção daquele arquivo é descartada.
- **Diff por padrão.** `shisho fix` mostra o diff; só `--apply` grava, e só se o arquivo não
  mudou desde a leitura.

## Alternativa registrada: OpenRewrite

Vale reavaliar se o Shishō passar a precisar de correções com **resolução de tipos** (ex.: mover
uma classe de pacote e atualizar todos os usos), que é onde o OpenRewrite brilha, ou se as
receitas de Spring voltarem a uma licença compatível. A porta `SourceFixer` permite trocar o
adaptador sem tocar no domínio.

## Consequências

- `shisho fix` corrige `non-final-dependency`, `field-injection`, `lombok-constructor`,
  `request-body-without-valid` e `transactional-read-only`. As demais lições continuam só
  explicadas (com o exemplo antes/depois do catálogo).
- Problemas conhecidos do `LexicalPreservingPrinter` com construções raras podem aparecer; a
  verificação de parse impede que virem código quebrado.

## 📚 Estudo — por que preservar o layout é difícil

Um parser comum transforma texto em árvore e **joga fora** o que não importa para a semântica:
espaços, quebras de linha, comentários soltos. Imprimir a árvore de volta gera um arquivo
"canônico", com outra indentação e outra ordem de quebras: o diff toca o arquivo inteiro.

Um **printer que preserva o layout** guarda, junto de cada nó, os tokens originais, inclusive os
espaços. Ao imprimir, os nós intactos saem exatamente como vieram, e só os nós alterados são
gerados de novo e encaixados no meio. É a mesma ideia do *Lossless Semantic Tree* do OpenRewrite e
do `LexicalPreservingPrinter` do JavaParser: a árvore é para o programa, os tokens são para o
humano que vai revisar o diff.

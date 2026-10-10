# ADR 0007 — Português e inglês: o idioma de quem lê e o idioma do código

- **Status:** aceito
- **Data:** 2026-10-09
- **Fase:** 8 (idiomas)

## Contexto

Até aqui o Shishō só falava português: relatórios, guias do catálogo e o código gerado pelo
`crud`. As mensagens da CLI já tinham versão em inglês, mas só como fallback. Para quem não lê
português, a ferramenta inteira era opaca, e o README em inglês precisava avisar disso.

São duas perguntas diferentes:

1. **Em que idioma o Shishō fala?** Relatórios, guias do modo sensei, explicações do `crud`,
   prompt do `ask`.
2. **Em que idioma está o código analisado?** Um projeto brasileiro pode ter pacotes `dominio`,
   `servico` e `repositorio`, e métodos `buscarPorId`. Um projeto em inglês usa `domain`,
   `service`, `findById`.

Elas são independentes. Um dev pode querer o relatório em inglês sobre código escrito em
português.

## Decisão

### 1. O idioma de quem lê: `SHISHO_IDIOMA`

O padrão continua sendo pt-BR, fixo e independente do locale do sistema. `SHISHO_IDIOMA=en`
troca tudo para inglês. A variável vai no `~/.bashrc`, junto do alias, e é isso que o
`install.sh` pergunta na instalação. Para uma vez só: `SHISHO_IDIOMA=en shisho analyze .`.

Um valor desconhecido é erro de entrada (código 2), e não silêncio: quem pediu `es` precisa
saber que não há espanhol.

Por que variável, e não opção na linha de comando: o idioma é uma preferência de quem usa, não
de cada análise, e a variável chega antes de o picocli montar a ajuda, que também precisa
estar no idioma certo.

### 2. Tradução do catálogo: `catalog/en/`, só o texto

O catálogo em português continua sendo a fonte da verdade: assinaturas, regras, custos,
coordenadas, referências. A tradução fica numa árvore paralela, `catalog/en/<pasta>/<id>.md`,
com o mesmo `id`. Ela traz **só o que é texto**: o corpo em Markdown e os campos de texto do
frontmatter (`nome`, `resumo`, `alternativa`). Os dados vêm sempre do arquivo em português.
Uma assinatura que muda num idioma e não no outro deixaria a classificação depender do idioma.

Os guias de kata em inglês usam as seções `## Why`, `## Acceptable when`, `## Before` e
`## After`.

Os templates de CRUD são a exceção que confirma a regra: neles o corpo **é** código, e o código
em inglês tem outros nomes (`ProductController`, `ErrorHandler`, `update()`). O template
traduzido traz também o `uma_vez_por_projeto`, porque os caminhos mudam. As dependências vêm do
português.

Um teste exige que todo documento do catálogo tenha tradução, e que nenhuma tradução fique
órfã. Sem tradução, o Shishō usaria o português, mas isso não chega a acontecer no catálogo do
repositório.

### 3. O idioma do código: alternativas nos padrões de pacote

Um segmento de padrão aceita alternativas separadas por `|`, como as dependências dos templates:
`domain|dominio`, `port/in|porta/entrada`. Assim um padrão continua sendo **um** sinal da
assinatura, e a porcentagem de confiança não cai porque o catálogo conhece dois idiomas. No
relatório aparece a primeira alternativa.

Os detectores e as katas que olham nomes de pacote (`domain`, `service`, `dto`) ganham os nomes
em português. As heurísticas por nome de método já eram bilíngues (`find`/`buscar`,
`Create`/`Criar`).

## Alternativas registradas

- **Uma opção `--idioma` em cada comando.** Repetida em todo comando, quando o idioma quase nunca
  muda. A variável cobre o caso pontual.
- **Usar o locale do sistema.** Muita máquina de dev roda em inglês por padrão, mesmo com o
  dono lendo português. O Shishō continua em pt-BR até alguém pedir outra coisa.
- **Catálogo duplicado inteiro em inglês.** Dobraria os dados, que acabariam divergindo.
- **Arquivo único com os dois idiomas.** O frontmatter ficaria cheio de `nome_en`, e o corpo
  precisaria de marcadores. Duas árvores são mais fáceis de revisar num PR.
- **Tradução automática na hora, pelo LLM.** O Shishō precisa ser útil com o LLM desligado.

## Consequências

- Uma contribuição ao catálogo precisa das duas versões. Quem só escreve num idioma abre o PR, e
  o teste mostra o que falta.
- O `crud` em inglês gera código em inglês. Misturar os dois num projeto é possível, mas o
  aviso de arquiteturas misturadas não percebe isso.

## 📚 Estudo — i18n e l10n

Internacionalizar (i18n) é preparar o programa para falar vários idiomas: nenhuma string de
usuário no código, tudo em arquivos por idioma. Localizar (l10n) é escrever cada idioma. O
Shishō já era internacionalizado desde a Fase 1, com o `messages.properties` como fallback, e por
isso trocar o idioma da CLI foi barato. O catálogo era o que faltava: conhecimento escrito em
prosa precisa de tradução de verdade, não de troca de chaves.

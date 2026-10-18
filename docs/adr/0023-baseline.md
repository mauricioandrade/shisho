# ADR 0023 — Baseline

- **Status:** aceito
- **Data:** 2026-10-18
- **Fase:** 22 (baseline)

## Contexto

O Shishō foi pensado para rodar em CI ([ADR 0010](0010-release.md)): o código de saída 1, com uma
lição GRAVE, reprova o job. Num projeto novo, isso funciona. Num projeto que já existe, não: dos
projetos da calibração ([ADR 0016](0016-real-projects.md)), o `macrozheng/mall` tem 310 lições, 223
delas graves. Quem liga o Shishō no CI de um projeto assim tem duas saídas ruins: consertar tudo
antes, o que ninguém faz, ou desligar o `fail-on-grave`, e aí o Shishō não barra mais nada.

## Decisão

### O comando e o arquivo

`shisho baseline <dir>` grava em `shisho-baseline.yaml`, na raiz do projeto, uma entrada por lição
que o projeto tem agora. O `analyze` lê o arquivo sozinho quando ele existe, como já faz com o
`shisho.yaml`, e por isso a GitHub Action e o hook do Claude Code passam a respeitá-lo sem nenhuma
opção nova. O arquivo foi feito para ser versionado junto com o código:

```yaml
licoes:
- kata: field-injection
  arquivo: mall-admin/src/main/java/com/macro/mall/config/MallSecurityConfig.java
  mensagem: kata.field-injection.lesson
  argumentos: [adminService]
```

### Como uma lição é reconhecida

Pela kata, pelo arquivo, pela chave da mensagem e pelos argumentos, que dizem o que a lição aponta:
o nome do campo, o tipo do corpo, a anotação. **Nunca pela linha.** Acrescentar um import no topo do
arquivo muda a linha de todas as lições abaixo dele, e com a linha na identidade todas virariam
novas. A severidade também fica de fora, para que mudá-la no `shisho.yaml` não crie lições novas.

Duas lições iguais no mesmo arquivo (dois `System.out.println` no mesmo método) viram duas
entradas iguais. Se aparece uma terceira, ela é a nova. Quando há mais lições do que entradas, as
últimas do arquivo são as novas. Às vezes a nova de verdade é outra, mas a conta fecha, e o CI
reprova do mesmo jeito.

### O que muda na análise

| | Com baseline |
|---|---|
| Terminal, hook do Claude Code | só as lições novas, mais uma linha com quantas conhecidas não foram mostradas e quantas foram corrigidas |
| Código de saída | 1 só com uma lição GRAVE **nova** |
| Graduação e nota | contam **todas** as lições: a baseline não melhora a nota |
| HTML | as novas, e as conhecidas num bloco recolhido (`<details>`) |
| SARIF | todas, com `baselineState` `new` ou `unchanged` |
| `fix` | corrige todas, as conhecidas inclusive |

A graduação conta tudo porque ela diz como o código está, e a baseline não muda o código. O SARIF
leva as conhecidas porque o code scanning do GitHub fecharia como "corrigido" um alerta que sumisse
do arquivo. O `baselineState` é o campo do próprio SARIF 2.1.0 para isso. O `fix` corrige tudo
porque é com ele que a dívida diminui. Quando uma lição da baseline some, o relatório avisa e pede
um `shisho baseline` novo. É o que faz a baseline só encolher.

No domínio, a `Baseline` faz a comparação (`compare`), e as portas `BaselineReader` e
`BaselineWriter` separam o arquivo da regra. O adaptador `YamlBaselineFile` implementa as duas.

### O que o corpus mostrou

No `mall`, gravar a baseline e analisar de novo deu 0 lições e código 0. Acrescentar cinco linhas
em branco no topo de um arquivo com lições não mudou nada. Uma injeção em campo nova deu código 1,
com só ela no relatório. Tirar o `@Autowired` de um campo conhecido deu a linha "1 lição da baseline
foi corrigida". Sem o arquivo, as saídas de terminal, SARIF e HTML dos dez projetos são as mesmas de
antes, byte a byte; no HTML mudaram só duas regras de CSS.

## Alternativas registradas

- **A linha na identidade**, ou um hash do texto da linha, como fazem algumas ferramentas. O hash
  aguenta o código se mover, mas um espaço reformatado já cria uma lição nova, e o Shishō não guarda
  o texto das linhas no modelo. Os argumentos dizem o mesmo que o texto da linha para quase todas as
  katas.
- **Uma opção `--baseline <arquivo>`** em vez de ler o arquivo sozinho. Cada lugar que roda o
  Shishō (a action, o hook, o CI de cada um) teria de passá-la, e quem esquecesse veria as 310
  lições de novo.
- **A graduação só com as lições novas.** Daria 1º dan ao `mall` no dia em que a baseline fosse
  gravada.
- **Uma opção `--sem-baseline`** para ver tudo. O HTML já mostra as conhecidas recolhidas, e o
  `fix` já as vê. Fica para quando alguém pedir.
- **Comparar só pelo diff do Git**, como "lições nas linhas alteradas". Exigiria o Git, ligaria o
  Shishō a um sistema de versão e não serviria no hook, que roda antes do commit.

## 📚 Estudo — a catraca

Uma **catraca** gira para um lado só. Aplicada à qualidade de código, a ideia é que a dívida
técnica pode ficar onde está, mas não pode crescer: todo código novo segue a regra, e o antigo é
corrigido aos poucos, quando alguém mexe nele. O SonarQube chama isso de *Clean as You Code*, e
ferramentas como o Detekt e o PMD têm baselines pelo mesmo motivo.

Funciona por uma questão de incentivo. Exigir que um time pare tudo para consertar 223 lições graves
não acontece. Exigir que ninguém crie a 224ª acontece, e custa pouco a cada PR. Com o tempo, a parte
do código que mais muda, que é também a que mais importa, fica limpa primeiro.

A catraca só funciona se a baseline encolher. Por isso o Shishō avisa quando uma lição dela foi
corrigida: uma baseline nunca regravada vira uma lista de exceções permanente, e aí ela deixa de ser
uma catraca e vira um tapete para varrer a dívida para baixo.

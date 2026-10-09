# ADR 0002 — Classificação de arquitetura e veredito

- **Status:** aceito
- **Data:** 2026-10-08
- **Fase:** 3

## Contexto

O catálogo (`catalog/architectures/*.md`) descreve cada arquitetura por uma `assinatura`,
`regras_dependencia`, um `custo`, as situações em que ela `faz_sentido_quando` e os
`sinais_overengineering` / `sinais_subengenharia` que indicam que ela não cabe no projeto. A
Fase 3 transforma isso em três respostas determinísticas:

1. **Com qual arquitetura o projeto se parece?**
2. **Ele respeita as regras de dependência dela?**
3. **Ela cabe no projeto?** (o veredito)

O catálogo foi escrito antes do código e mistura entradas que dá para checar só com o que o
parser já extrai (`domain/model`, `@KafkaListener`, `org.springframework`) com outras que pedem
mais informação (`<modulo>/internal`, `banco:<outro-servico>`, `docker-compose_com_varios_servicos_spring`).
O princípio do §7 do CLAUDE.md vale: **o catálogo pode andar na frente do código**.

## Decisão

### 1. Classificação

Para cada arquitetura, contam-se os **sinais** da assinatura presentes no projeto:

- **Padrão de pacote** (`assinatura.pacotes`): segmentos separados por `/` que aparecem
  **consecutivos** no nome de algum pacote. `domain/model` casa com `dev.app.domain.model.user`,
  não com `dev.app.domain.port.model`.
- **Placeholder** (`<feature>/controller`, `<modulo>/api`) casa com qualquer segmento, mas o
  padrão só conta como sinal se aparecer com **pelo menos dois valores diferentes**. Sem isso,
  `com.loja.controller` (camadas) casaria com `<feature>/controller` (por funcionalidade) com
  `<feature> = loja`.
- **Marcador** (`assinatura.anotacoes`), pela forma:
  - começa com maiúscula ou `@` → anotação, pelo nome simples (`@KafkaListener`, `CommandHandler`);
  - tem ponto → prefixo de import ou de `groupId` (`org.axonframework`, `org.springframework.modulith`);
  - o resto → prefixo de `artifactId` (`spring-kafka`, `spring-cloud`);
  - tem `_` → ID de detector ainda não implementado; nunca casa.

É candidata a arquitetura que atinge `minimo_sinais`. Vence a de **maior proporção** da
assinatura encontrada (`confiança` = sinais ÷ entradas da assinatura); empate vai para quem tem
mais sinais e, depois, para o `id` em ordem alfabética.

### 2. Regras de dependência

Só as regras da arquitetura detectada são checadas, contra os **imports** de cada classe cujo
pacote casa com `de`:

- entrada com ponto (`org.springframework`, `jakarta.persistence`) → prefixo do import;
- outra entrada (`infrastructure`, `controller`) → padrão de pacote, aplicado ao pacote **do
  projeto** para onde o import aponta. Um import de biblioteca nunca casa com um padrão de pacote.

Regras com placeholder ou qualificador (`<modulo>`, `banco:`, `codigo:`, `ciclo:`) precisam de
mais que imports e não são checadas ainda. Cada violação é uma lição de id `dependency-rule`
(MEDIA), configurável no `shisho.yaml` como qualquer kata.

### 3. Veredito

Os detectores (`infrastructure/detector`) medem os sinais listados **na arquitetura detectada**.
Sinal sem detector é ignorado (log em DEBUG). A ordem:

1. algum sinal de **subengenharia** medido → **subengenharia**; sugere `evolui_para`;
2. algum sinal de **overengineering** medido, **ou** contexto declarado sem nenhuma chave de
   `faz_sentido_quando`, numa arquitetura de complexidade média ou alta → **overengineering**;
   sugere `alternativa_mais_simples`;
3. senão → **adequada**, citando as chaves do contexto que pagam o custo.

Sem `contexto` no `shisho.yaml`, só os sinais medidos contam, e o relatório pede o contexto.

O contexto vira chaves do catálogo: `tipo: mvp` → `mvp`; `vida_util: longa` →
`vida_util_longa`; `time` ≥ 10 → `time_grande`; `caracteristicas` lista as demais chaves direto.

### Detectores implementados

`um_use_case_por_metodo_crud`, `dominio_anemico`, `entidade_de_dominio_identica_a_entidade_jpa`,
`mappers_entre_estruturas_identicas`, `pacote_service_com_mais_de_15_classes`, `service_deus`,
`dependencias_ciclicas_entre_services`. Escolhidos por serem mensuráveis só com a estrutura
(nomes, campos, anotações, tipos de parâmetro), sem corpo de método nem histórico do git.

## Alternativas consideradas

- **Pontuação ponderada por sinal** (ex.: `domain/port` vale mais que `domain`). Mais precisa,
  mas os pesos viveriam no código, não no catálogo, e seriam arbitrários. A proporção simples
  é explicável em uma linha do relatório.
- **Veredito por pontuação contínua** (custo medido × contexto). Mais nuance, mas difícil de
  explicar e de testar. Regras em ordem fixa dão um veredito auditável: dá para dizer
  exatamente qual sinal o causou.
- **Classes por funcionalidade como fato do veredito.** O catálogo tem `custo.classes_por_feature`,
  mas medir "funcionalidade" (entidades? controllers?) é impreciso. Fica para quando houver um
  detector confiável; por ora o custo aparece só como referência no texto do veredito.
- **Grafo de pacotes como objeto do domínio.** As regras de hoje só precisam dos imports de
  cada classe. O grafo vira um tipo quando um detector precisar de ciclos entre pacotes
  (`dependencias_ciclicas_entre_features`).
- **Avisar no terminal cada sinal sem detector.** Seriam ~25 avisos em toda análise, para o
  usuário que não pode fazer nada a respeito. O aviso vai para o log em DEBUG; quem contribui
  com o catálogo vê no teste `DetectorsTest` quais IDs existem.

## Consequências

- O Shishō se classifica como Clean Architecture (100%), passa nas regras dela e, com o
  `shisho.yaml` do repositório, julga a própria arquitetura adequada.
- Projetos de um pacote só (como o fixture "aluno exemplar") não se parecem com nenhuma
  arquitetura do catálogo. É honesto: há pouca estrutura para classificar.
- Um projeto pode ter uma arquitetura no papel e outra na prática; a classificação vê só a
  estrutura de pacotes e marcadores, não a intenção.

## 📚 Estudo — overengineering é uma relação, não uma propriedade

Nenhum código é "overengineered" sozinho. Uma Clean Architecture com portas, adaptadores e um
use case por operação é exatamente o que um domínio de seguros com dez integrações precisa, e
exatamente o que um CRUD de portfólio não precisa. Por isso o veredito cruza dois lados:

- **o custo** da arquitetura, que está no catálogo (`custo`) e em fatos medidos no código: use
  cases que só repetem o CRUD, entidades JPA idênticas ao domínio, mappers entre estruturas
  iguais. Cada um desses é uma classe a mais que não carrega decisão nenhuma;
- **o retorno** que o contexto promete (`faz_sentido_quando`): regras de domínio complexas,
  infraestrutura que vai mudar, vida longa, time grande. É isso que paga as classes a mais.

Sem retorno declarado, o custo é só custo. A regra prática que o catálogo resume: comece pela
opção mais simples que atende aos requisitos e suba um degrau quando um sinal concreto de
subengenharia aparecer: um service com 30 métodos, services que dependem um do outro, um pacote
que ninguém mais consegue navegar.

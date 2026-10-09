# Catálogo de Arquiteturas do Shishō

Cada arquivo em `architectures/` descreve **um** estilo arquitetural. O frontmatter YAML é lido
pelo Shishō (detecção, regras de dependência, veredito de overengineering); o corpo em Markdown
é a explicação didática usada nas lições e como contexto para o LLM local (opcional).

Contribuir com uma arquitetura nova = abrir um PR com um único arquivo neste formato.

## Schema do frontmatter

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | string | Identificador único, igual ao nome do arquivo |
| `nome` | string | Nome legível |
| `familia` | enum | `simples` · `modular` · `isolamento-de-dominio` · `distribuida` |
| `assinatura.pacotes` | lista | Nomes de pacote que indicam o estilo |
| `assinatura.anotacoes` | lista | Anotações/dependências que indicam o estilo (opcional) |
| `assinatura.minimo_sinais` | int | Quantos sinais precisam bater para o estilo ser candidato |
| `regras_dependencia` | lista | `de` → `nao_pode_depender_de`; violações viram lições |
| `custo.classes_por_feature` | faixa | Estimativa de classes para uma feature típica (CRUD + 1 regra) |
| `custo.complexidade` | enum | `baixa` · `media` · `alta` |
| `faz_sentido_quando` | lista | Chaves de contexto (ver abaixo) que justificam o custo |
| `sinais_overengineering` | lista | IDs de detectores que indicam custo sem retorno |
| `sinais_subengenharia` | lista | IDs de detectores que indicam que o estilo já não comporta o projeto |
| `evolui_para` | lista | Próximos estilos naturais quando o projeto cresce |
| `alternativa_mais_simples` | id | Para onde recuar se for overengineering |
| `referencias` | lista | Fontes primárias |

### Como a assinatura e as regras são lidas

Detalhes e motivos em [`docs/adr/0002-architecture-classification.md`](../docs/adr/0002-architecture-classification.md).

- **Padrão de pacote** (`pacotes`, `de`, `nao_pode_depender_de`): segmentos separados por `/`
  que aparecem consecutivos no nome do pacote. `domain/model` casa com `dev.app.domain.model`.
- **Sufixo de classe** (`*Controller`, só como último segmento): o padrão é aplicado ao nome
  qualificado das classes, e não ao pacote. `<feature>/*Controller` casa com
  `com.loja.usuario.UsuarioController`: a classe termina em `Controller` e está direto no
  pacote `usuario`.
- **Placeholder** (`<feature>`, `<modulo>`): qualquer segmento. Na assinatura, só conta com
  pelo menos dois valores diferentes (duas features, dois módulos). Regras com placeholder ou
  qualificador (`banco:`, `codigo:`, `ciclo:`) ainda não são checadas.
- **Marcador** (`anotacoes`): com maiúscula ou `@`, uma anotação (`@KafkaListener`); com
  ponto, prefixo de import ou de `groupId` (`org.axonframework`); senão, prefixo de
  `artifactId` (`spring-kafka`). Com `_`, um ID de detector ainda não implementado.
- **Regra de dependência**: entrada com ponto (`org.springframework`) é prefixo de import;
  as demais são padrões aplicados aos pacotes do próprio projeto.

### Chaves de contexto (`faz_sentido_quando`)

Correspondem ao que o autor declara na seção `contexto` do `shisho.yaml` (diretamente em
`caracteristicas`, ou derivadas de `tipo`, `time` e `vida_util`). Um teste garante que o
catálogo só usa estas:

`dominio_com_regras_complexas`, `multiplas_integracoes_externas`, `troca_provavel_de_infra`,
`vida_util_longa`, `time_grande`, `multiplos_times`, `carga_leitura_escrita_assimetrica`,
`escala_independente_por_dominio`, `processamento_assincrono`, `auditoria_de_eventos`,
`crud_simples`, `mvp`, `muitas_features`.

### IDs de detectores

São implementados em Java (`infrastructure/detector`). IDs sem detector são ignorados (com um
aviso no log em nível DEBUG), então o catálogo pode documentar um sinal antes de o detector
existir. Implementados hoje: `um_use_case_por_metodo_crud`, `dominio_anemico`,
`entidade_de_dominio_identica_a_entidade_jpa`, `mappers_entre_estruturas_identicas`,
`pacote_service_com_mais_de_15_classes`, `service_deus`, `dependencias_ciclicas_entre_services`.

## Recursos do Java (`java-features/`)

Cada arquivo descreve um recurso da linguagem ou da biblioteca padrão, a partir de quando ele
existe e se já é final. É o que torna o Shishō consciente de versão: uma kata que sugere um
recurso declara o `id` dele, e o teste exige que a versão mínima da kata seja o `desde` daqui.

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | string | Identificador único, igual ao nome do arquivo |
| `nome` | string | Nome legível |
| `desde` | int | Versão do Java em que o recurso é final (ou em que está em preview) |
| `status` | enum | `final` · `preview` |
| `jep` | int | Número do JEP |

O corpo explica o recurso (`## O que é`) e mostra um `## Exemplo`.

Num projeto abaixo do `desde`, a kata ainda roda, mas cada lição vira **INFO** ("Ao migrar
para o Java 25: …"), sem tirar nota. Recursos em **preview nunca** são sugeridos: nenhuma kata
pode apontar para um.

## Dependências a evitar (`dependencies/`)

Cada arquivo descreve uma biblioteca que o Shishō desaconselha: abandonada, substituída por um
sucessor que o próprio projeto recomenda, ou publicada em coordenadas novas. A kata
`avoided-dependency` procura as `coordenadas` no build do projeto analisado.

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | string | Identificador único, igual ao nome do arquivo |
| `nome` | string | Nome legível |
| `coordenadas` | lista | `groupId:artifactId`; qualquer uma delas no build gera a lição |
| `resumo` | string | O motivo em uma frase, mostrado na lição |
| `alternativa` | string | O que usar no lugar, mostrado na lição |
| `referencias` | lista | Fontes primárias (URLs `https://`): o anúncio do próprio projeto, notas de versão |

O corpo explica o motivo (`## Por quê`). Só entra aqui o que tem fonte verificável. Uma
biblioteca de que alguém não gosta não entra: o critério é o próprio projeto ter declarado
abandono, sucessor ou mudança de coordenadas. Uma mesma coordenada não pode aparecer em dois
arquivos.

## Guias das katas (`katas/`)

Cada arquivo em `katas/` ensina uma kata: **por que** ela existe, **quando** o código que ela
acusa é uma escolha razoável, como corrigir (o código **antes** e **depois**) e as **fontes**.
É o que o modo sensei (`--sensei`), o relatório HTML e a ajuda das regras no SARIF mostram.

Uma kata aponta um custo, não um pecado. O guia explica a troca: o que se ganha seguindo a
prática, e em que contexto o ganho não compensa.

```markdown
---
id: field-injection        # igual ao nome do arquivo e ao id da kata
referencias:               # fontes primárias: documentação oficial, JEPs, mantenedores
  - https://docs.spring.io/...
---

## Por quê

Um ou mais parágrafos. Sem listas: cada parágrafo vira um bloco de texto no relatório.

## Quando é aceitável

Os contextos em que o código acusado faz sentido.

## Antes

```java
...
```

## Depois

Texto opcional explicando a correção.

```java
...
```
```

As quatro seções e `referencias` (URLs `https://`) são obrigatórias, assim como um bloco de
código em "Antes" e em "Depois". Um teste roda cada kata sobre o próprio exemplo: o "Antes"
precisa gerar a lição e o "Depois" não pode gerar nenhuma. Toda kata precisa de um guia.

## Famílias e o caminho natural de evolução

```
layered ──► package-by-feature ──► modular-monolith ──► microservices
                                         │
                       hexagonal / clean / onion  (isolamento de domínio, dentro de cada módulo)
                                         │
                       cqrs / event-driven        (táticas pontuais, não ponto de partida)
```

A regra que o Shishō aplica: **comece pela opção mais simples que atende aos requisitos
declarados e só suba um degrau quando houver um sinal concreto de subengenharia.**

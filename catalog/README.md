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

### Chaves de contexto (`faz_sentido_quando`)

Correspondem ao que o autor declara em `shisho.yaml` e ao que o Shishō mede:

`dominio_com_regras_complexas`, `multiplas_integracoes_externas`, `troca_provavel_de_infra`,
`vida_util_longa`, `time_grande`, `multiplos_times`, `carga_leitura_escrita_assimetrica`,
`escala_independente_por_dominio`, `processamento_assincrono`, `auditoria_de_eventos`,
`crud_simples`, `mvp`, `muitas_features`.

### IDs de detectores

São implementados em Java (`infrastructure/detector`). IDs desconhecidos são ignorados com aviso,
então o catálogo pode documentar um sinal antes de o detector existir.

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

---
id: package-by-feature
nome: Pacotes por funcionalidade (Package by Feature)
familia: simples
assinatura:
  # Plano (usuario/UsuarioController) ou com subpacotes (usuario/controller/...):
  # pacotes com nomes de domínio contendo classes de papéis técnicos diferentes.
  pacotes: ["<feature>/*Controller|*Controlador", "<feature>/*Service|*Servico", "<feature>/*Repository|*Repositorio",
            "<feature>/controller|controlador", "<feature>/service|servico", "<feature>/repository|repositorio"]
  minimo_sinais: 2
regras_dependencia:
  - de: "<feature>"
    nao_pode_depender_de: ["<outra-feature>/repository|repositorio", "<outra-feature>/entity|entidade"]
    # features conversam pelo service público, não pelo banco umas das outras
custo:
  classes_por_feature: 4-6
  complexidade: baixa
faz_sentido_quando:
  - crud_simples
  - mvp
  - muitas_features
sinais_overengineering: []
sinais_subengenharia:
  - feature_acessa_repository_de_outra
  - entidade_jpa_compartilhada_entre_features
  - dependencias_ciclicas_entre_features
evolui_para: [modular-monolith]
alternativa_mais_simples: layered
referencias:
  - http://www.javapractices.com/topic/TopicAction.do?Id=205
---

## O que é

O primeiro nível de pacotes é o **assunto** (`usuario`, `pedido`, `pagamento`); dentro de cada
um, as classes técnicas daquela feature. Mesma simplicidade do layered, coesão muito maior.

## Por que costuma ser o melhor padrão inicial

Tudo que muda junto fica junto. Apagar uma feature é apagar um pacote. Dá para deixar classes
como package-private e expor só o service, o que já cria fronteiras sem nenhum framework.

## Onde começa a doer

Quando as features passam a ler as entidades umas das outras e surgem ciclos, as fronteiras
viraram decorativas. Esse é o sinal para formalizá-las com um **monolito modular**.

## Relação com Clean/Hexagonal

Não são excludentes: dá para ter `pedido/domain`, `pedido/application`, `pedido/infrastructure`.
O Shishō reporta essa combinação como "package-by-feature + hexagonal".

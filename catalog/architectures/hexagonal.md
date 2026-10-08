---
id: hexagonal
nome: Hexagonal (Ports & Adapters)
familia: isolamento-de-dominio
assinatura:
  pacotes: [domain, port, adapter, "port/in", "port/out", "adapter/in", "adapter/out"]
  minimo_sinais: 2
regras_dependencia:
  - de: domain
    nao_pode_depender_de: [adapter, infrastructure, "org.springframework", "jakarta.persistence"]
  - de: application
    nao_pode_depender_de: [adapter]
custo:
  classes_por_feature: 6-10   # porta in/out, use case, adapters, mapper, DTOs
  complexidade: media
faz_sentido_quando:
  - multiplas_integracoes_externas
  - dominio_com_regras_complexas
  - troca_provavel_de_infra
  - vida_util_longa
sinais_overengineering:
  - portas_com_uma_implementacao_sem_fake_em_teste
  - use_cases_que_so_delegam
  - mappers_entre_estruturas_identicas
  - dominio_anemico
sinais_subengenharia: []
evolui_para: [modular-monolith]
alternativa_mais_simples: package-by-feature
referencias:
  - https://alistair.cockburn.us/hexagonal-architecture/
---

## O que é

A aplicação tem um **núcleo** (domínio + casos de uso) que não conhece nada do mundo externo.
Ele define **portas** (interfaces): de entrada, para quem o aciona, e de saída, para o que ele
precisa (persistência, e-mail, APIs). **Adaptadores** implementam essas portas: um controller
REST, um repositório JPA, um cliente HTTP.

## Quando vale o custo

Quando o núcleo tem regra de negócio de verdade e a infraestrutura é volátil: várias
integrações externas, chance real de trocar banco ou fornecedor, necessidade de testar o
domínio sem subir Spring. A porta de saída permite um fake em memória nos testes.

## Quando é exagero

Se o domínio é um CRUD sem regras, as portas protegem um núcleo que não existe: o use case só
chama a porta, a porta só chama o repository, e cada campo é copiado por três mappers. Sinal
clássico: interfaces com exatamente uma implementação que nenhum teste substitui.

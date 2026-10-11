---
id: layered
nome: Camadas técnicas (Layered)
familia: simples
assinatura:
  pacotes: ["controller|controlador|resource|recurso", "service|servico", "repository|repositorio", "model|modelo", "entity|entidade", dto]
  minimo_sinais: 3
regras_dependencia:
  - de: "repository|repositorio"
    nao_pode_depender_de: ["controller|controlador|resource|recurso", "service|servico"]
  - de: "service|servico"
    nao_pode_depender_de: ["controller|controlador|resource|recurso"]
custo:
  classes_por_feature: 4-6
  complexidade: baixa
faz_sentido_quando:
  - crud_simples
  - mvp
sinais_overengineering: []
sinais_subengenharia:
  - pacote_service_com_mais_de_15_classes
  - service_deus                  # service com muitos métodos e dependências
  - regra_de_negocio_no_controller
  - dependencias_ciclicas_entre_services
  - mudanca_de_feature_toca_todos_os_pacotes
evolui_para: [package-by-feature]
alternativa_mais_simples: null
referencias:
  - https://martinfowler.com/bliki/PresentationDomainDataLayering.html
---

## O que é

O código é agrupado pelo **papel técnico**: todos os controllers juntos, todos os services
juntos, todos os repositories juntos. É a estrutura que o Spring Initializr e a maioria dos
tutoriais sugerem.

## Quando é a escolha certa

Para um CRUD pequeno, um MVP ou um projeto de estudo, é imbatível: zero cerimônia, qualquer
dev Spring entende na hora.

## Onde começa a doer

O problema aparece com o crescimento. Uma feature ("pedido") fica espalhada por quatro pacotes,
o pacote `service` vira uma gaveta com dezenas de classes sem relação entre si, e nada impede
que `PedidoService` chame `UsuarioRepository` direto. A coesão é por tipo, não por assunto.

## Próximo degrau

Quando o Shishō detecta sinais de subengenharia, o passo natural é **package-by-feature**:
manter as mesmas camadas, só que dentro de cada feature.

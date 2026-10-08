---
id: modular-monolith
nome: Monolito Modular
familia: modular
assinatura:
  pacotes: ["<modulo>/api", "<modulo>/internal", "<modulo>/events"]
  anotacoes: ["org.springframework.modulith", "@ApplicationModule", "package-info.java"]
  minimo_sinais: 2
regras_dependencia:
  - de: "<modulo>"
    nao_pode_depender_de: ["<outro-modulo>/internal"]
  - de: "<modulo>"
    nao_pode_depender_de: ["ciclo:<outro-modulo>"]   # sem ciclos entre módulos
custo:
  classes_por_feature: 5-8
  complexidade: media
faz_sentido_quando:
  - muitas_features
  - vida_util_longa
  - time_grande
  - dominio_com_regras_complexas
sinais_overengineering:
  - modulo_unico_ou_dois_modulos
  - eventos_entre_modulos_sem_consumidor_real
  - api_de_modulo_que_so_repassa_repository
sinais_subengenharia:
  - modulos_com_requisitos_de_escala_divergentes
  - deploys_bloqueados_por_times_diferentes
evolui_para: [microservices]
alternativa_mais_simples: package-by-feature
referencias:
  - https://spring.io/projects/spring-modulith
  - https://martinfowler.com/bliki/MonolithFirst.html
---

## O que é

Um único deploy, mas dividido em **módulos com fronteiras verificadas**: cada módulo expõe uma
API pública pequena e esconde o resto. A comunicação entre módulos acontece pela API ou por
eventos de aplicação. No ecossistema Spring, o Spring Modulith verifica essas regras em teste.

## Por que é o "meio-termo saudável"

Entrega quase todos os benefícios de organização que as pessoas buscam em microsserviços
(fronteiras claras, times independentes no código) sem rede, sem consistência eventual e sem
infraestrutura distribuída. Se um dia um módulo precisar sair, a fronteira já está pronta.

## Quando é exagero

Com dois ou três módulos pequenos e um dev só, a formalização (eventos, `package-info`,
verificação) custa mais do que o package-by-feature, que já dá conta.

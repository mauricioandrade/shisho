# ADR 0005 — `shisho crud`: CRUD gerado a partir de templates do catálogo

- **Status:** aceito
- **Data:** 2026-10-09
- **Fase:** 7 (gerador de CRUD)

## Contexto

Até a Fase 6, o Shishō só olhava para o código que já existe. A Fase 7 ensina **antes** de o
código existir. `shisho crud Usuario nome:String email:String` cria, na arquitetura do projeto,
tudo que um CRUD REST precisa: entidade, repositório, DTOs, service, controller e tratamento de
erros. E explica o papel de cada arquivo.

O caso típico: o projeto acabou de sair do Spring Initializr, que resolve o build e as
dependências, mas não diz onde cada classe vai nem por quê.

Restrições do CLAUDE.md:

- **O catálogo é a fonte da verdade.** O que cada arquitetura gera não pode ficar hardcoded em Java.
- **Dogfooding.** O código gerado tem de passar em todas as katas.
- **Simplicidade.** Nada de motor de templates genérico nem dependência nova.

## Decisão

### 1. Templates em Markdown, em `catalog/scaffolds/<arquitetura>.md`

Um arquivo por arquitetura, com o mesmo `id` dela. Cada `## caminho/Arquivo.java` é um arquivo
gerado: o texto antes do bloco de código é a explicação mostrada no terminal, e o bloco é o
conteúdo. O frontmatter declara as dependências que o código exige (`dependencias`, por
`artifactId`) e os arquivos que existem uma vez por projeto (`uma_vez_por_projeto`). Esses
arquivos são pulados se o projeto já tem uma classe com a anotação indicada, como o
`@RestControllerAdvice`.

Quem conhece uma arquitetura contribui com o template sem escrever Java, do mesmo jeito que
contribui com a explicação dela.

### 2. Placeholders mínimos, sem motor de templates

- `{{pacote}}`, `{{Entidade}}`, `{{entidade}}`, `{{feature}}`, `{{rota}}`;
- `{{#campos}}…{{/campos}}` repete o trecho para cada campo, com `{{campo}}`, `{{Campo}}`,
  `{{tipo}}` e `{{validacao}}`; `{{,}}` vira vírgula entre os campos;
- `{{#importacoes}}` e `{{#validacoes}}` listam os imports que os tipos e as validações pedem.

Uma linha que só tem uma tag de seção some, e três quebras de linha seguidas viram duas. Sem
condicionais nem aninhamento: são cerca de 60 linhas de Java, testadas, contra uma dependência
como Mustache ou Freemarker.

### 3. O que é de Java fica no adaptador

Os tipos aceitos nos campos (`String`, `Long`, `BigDecimal`, `LocalDate`…), o import de cada um e
a validação (`@NotBlank` para texto, `@NotNull` para o resto) ficam no adaptador
`infrastructure/scaffold`. O `src/main/java` também. O caso de uso (`GenerateCrudService`)
só sabe escolher a arquitetura e o template, achar o pacote base e decidir o que pular.

**Pacote base:** o prefixo comum a todos os pacotes do projeto. Num projeto do Initializr é o
pacote da classe `Application`. Funciona sem citar Spring no domínio, e `--pacote` resolve os
casos ambíguos.

**Arquitetura:** a detectada, ou a de `--arquitetura`. Se as duas existem e são diferentes, o
Shishō gera o que foi pedido e avisa que o projeto vai misturar estilos.

### 4. Grava direto, nunca sobrescreve

Ao contrário do `fix`, que mexe em código existente e por isso mostra o diff antes, o `crud`
só **cria** arquivos novos. Se algum arquivo da entidade já existe, nada é gravado. Ao final, o
Shishō lista as dependências que faltam no build e lembra de rodar `shisho analyze`.

### 5. Primeiro `layered` e `package-by-feature`

São os pontos de partida que o catálogo recomenda. O `package-by-feature` é gerado **plano**
(`usuario/UsuarioController.java`), com classes package-private: é o que o texto do catálogo
defende ("expor só o service").

Isso expôs uma lacuna. A assinatura procurava `<feature>/controller` (subpacotes), e um projeto
plano não era reconhecido. A assinatura passa a aceitar **padrões de sufixo de classe**:
`<feature>/*Controller` casa com `com.loja.usuario.UsuarioController`. A regra do placeholder
continua, com pelo menos duas features diferentes: com uma só, não dá para distinguir de um
pacote qualquer.

## O teste-oráculo

Para cada template, o teste parte de um projeto igual ao do Initializr, gera duas entidades com
todos os tipos de campo e roda o `analyze`. Exige três coisas: a arquitetura reconhecida é a
pedida, não há nenhuma violação de regra de dependência e não há **nenhuma** lição. O Shishō
julga o que ele mesmo gera, e um template que sai do padrão quebra o build.

## Alternativas registradas

- **Templates em Java** (StringBuilder ou JavaPoet). Seguros em tipos, mas tiram o conhecimento
  do catálogo e afastam quem quer contribuir sem programar em Java.
- **Mustache/Freemarker.** Condicionais e laços aninhados que os templates de hoje não usam,
  por uma dependência nova.
- **Gerar o projeto inteiro (chamar o Initializr).** O Initializr já resolve build e
  dependências muito bem. O Shishō entra depois dele.
- **`package-by-feature` com subpacotes** (`usuario/controller`). Já casaria com a assinatura
  antiga, mas obrigaria as classes a serem públicas e contrariaria o próprio catálogo.
- **Avisar overengineering antes de gerar** ("você pediu hexagonal para um MVP"). Só faz sentido
  quando houver templates de arquiteturas de custo médio ou alto. Entra com o template
  hexagonal ou clean.

## Consequências

- `shisho crud [--arquitetura] [--pacote] [--rota] <dir> <Entidade> campo:Tipo...`.
- Conferido num projeto real do Spring Initializr (Spring Boot 4.1): o código gerado compila, o
  contexto sobe e a API responde. O teste real revelou duas coisas, já corrigidas:
  - o Boot 4 renomeou `spring-boot-starter-web` para `spring-boot-starter-webmvc`, então uma
    entrada de `dependencias` aceita alternativas separadas por `|`;
  - o 400 de validação só dizia "Invalid request content.". O `TratadorDeErros` gerado agora lista
    os campos que falharam.
- Um projeto package-by-feature plano com duas ou mais features passa a ser reconhecido. Com
  uma feature só, o segundo `crud` precisa de `--arquitetura` de novo.
- O schema de `catalog/scaffolds` fica documentado em `catalog/README.md`.

## 📚 Estudo — scaffolding que ensina

Geradores de código costumam esconder decisões: o arquivo aparece, ninguém sabe por quê. O
`crud` faz o contrário: cada arquivo vem com o motivo de existir, e o resultado passa pelo mesmo
crivo de qualquer código, as katas. É a diferença entre copiar uma receita e entender a cozinha.

O código gerado é **ponto de partida**, não framework: não há classe base mágica nem anotação
própria do Shishō. Depois de gerado, o código é seu, e o `analyze` continua cobrando dele o
mesmo que de qualquer outro.

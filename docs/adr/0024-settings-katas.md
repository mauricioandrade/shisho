# ADR 0024 — Katas de configuração

- **Status:** aceito
- **Data:** 2026-10-18
- **Fase:** 23 (katas de configuração)

## Contexto

Até aqui o Shishō lia duas fontes: o código Java e o arquivo de build. Uma terceira, que todo
projeto Spring Boot e Quarkus tem, ficava de fora: o `application.properties` (ou `.yml`). É nele
que moram alguns dos erros mais caros de produção, e eles não aparecem no código. Um
`ddl-auto=create-drop` apaga o banco a cada deploy. Uma senha escrita no arquivo vai para o Git.

## Decisão

### O modelo

O `ProjectModel` ganhou `settings`: os `SettingsFile`, cada um com uma lista de `Setting` (chave,
valor, linha e perfis). O domínio não sabe o que é Spring nem Quarkus: são chaves e valores. O
nome `Settings` evita confusão com `ProjectConfig`, que é o `shisho.yaml`, a configuração do
próprio Shishō.

O adaptador `ApplicationSettingsReader` (pacote `infrastructure/settings`, porta `SettingsReader`)
lê os `application*.properties`, `.yml` e `.yaml` de cada `src/main/resources` (e do `config/` dentro
dele), em todos os módulos. Fora disso, nada: `src/test`, build e diretórios ocultos ficam de fora.

- **`.properties`** é lido como o `java.util.Properties` lê (separadores `=`, `:` ou espaço,
  continuação com `\`, escapes), mas guardando a linha de cada chave, que o `Properties` perde.
- **YAML** é lido com o SnakeYAML, que já era dependência, pela árvore de nós (`compose`), que
  guarda a linha de cada chave. As chaves aninhadas viram uma só com pontos, e os itens de lista
  viram `chave[0]`, a forma que o Spring usa.
- **Perfis** vêm do nome do arquivo (`application-prod.yml`), do prefixo do Quarkus
  (`%dev,test.chave`) e do `spring.config.activate.on-profile` de cada documento (`---` no YAML,
  `#---` no `.properties`).

Um arquivo que não dá para ler fica de fora, sem erro: a configuração nunca impede a análise.

### As katas

| Kata | Severidade | Regra | No corpus |
|---|---|---|---|
| `destructive-schema-generation` | GRAVE | `ddl-auto` (ou `hbm2ddl.auto`) `create`/`create-drop`, ou `drop-and-create`/`drop` no JPA e no Quarkus | 7 quickstarts com `drop-and-create` sem prefixo |
| `hardcoded-secret` | MEDIA | a última palavra da chave é `password`, `secret`, `token`, `api key`, `private key`… e o valor é literal | 62: mall (14), eladmin (5), jhipster (2), realworld (1), quickstarts (40) |
| `open-in-view` | MEDIA | um módulo com `spring-boot-starter-data-jpa` cujas configurações não declaram `spring.jpa.open-in-view` | buckpal |
| `sql-logging` | LEVE | `show-sql=true`, `log.sql=true` no Quarkus, ou o logger `org.hibernate.SQL` em `debug`/`trace` | 10 quickstarts |

Três delas só olham o que **produção lê**. Uma configuração cujos perfis são todos de
desenvolvimento ou teste (`dev`, `development`, `local`, `test`) fica de fora, porque ali o
`create-drop` e o `show-sql` são exatamente o que se quer. No Quarkus, uma chave sem prefixo vale
também em produção, a não ser que um `%prod.` **da mesma aplicação** a substitua. O Quarkus tem um
perfil de produção com nome fixo (`prod`). No Spring, o nome é livre, e o Shishō não tem como
saber qual perfil é o de produção.

As chaves são comparadas como o *relaxed binding* do Spring: `open-in-view`, `openInView` e
`open_in_view` são a mesma.

A `hardcoded-secret` decide pela **última palavra** da chave: `login-password` e `jwt.secret` são
segredos, `token-validity-in-seconds` não. Um placeholder (`${DB_PASSWORD}`, mesmo com padrão), um
valor vazio ou um booleano não são literais. A lição nunca repete o valor, para não espalhar o
segredo pelo relatório, pelo SARIF e pelo log do CI.

A `open-in-view` aceita os dois valores, desde que escritos. Ela acusa o padrão que ninguém
escolheu, como a `to-one-without-lazy` ([ADR 0021](0021-jpa-katas.md)). Um módulo herda a
dependência do pom pai, então as configurações dos módulos dentro dele também contam. Ela não
acusa um módulo que importa um config server (`spring.config.import=configserver:`), porque a
propriedade pode estar lá. É o caso dos quatro serviços com JPA do petclinic-microservices.

### O que o corpus mostrou

- **Uma contagem errada na calibração.** A primeira rodada contou 0 lições de schema nos
  quickstarts, depois de contar 7 numa rodada anterior. Analisado como um projeto só, o repositório
  junta dezenas de aplicações, e um `%prod.` de uma delas escondia o `drop-and-create` das outras. A
  substituição passou a valer só dentro da mesma aplicação (o mesmo `src/main/resources`).
- **Um falso positivo no eladmin.** Ele declara `open-in-view: true` no módulo `eladmin-system`,
  mas o starter está no pom pai. A kata olhava só os arquivos do pai. Agora olha também os dos
  módulos de dentro.
- **Conferido e certo:** três segredos do corpus são textos de modelo ("填写你的AccessKey", ou
  "preencha sua AccessKey", e "your appPrivateKey"). Hoje não expõem nada, mas o valor real vai
  parar no mesmo arquivo commitado, e a lição continua valendo. Os `%prod.quarkus.datasource.password`
  dos quickstarts são literais de verdade, num perfil de produção.

Sem as katas novas, as lições dos nove projetos são as mesmas da 0.3.0. Os fixtures "aluno
exemplar", "astronauta", `initializr` e `demo-loja` passaram a declarar `spring.jpa.open-in-view=false`,
como um projeto bem configurado faz. O "aluno bagunçado" ganhou a lição.

## Alternativas registradas

- **Ler o `application.properties` com o `java.util.Properties`.** Ele perde a linha de cada chave,
  e uma lição sem linha não mostra onde está o problema.
- **Uma biblioteca de configuração** (o próprio Spring Boot, o SmallRye Config). Resolveria
  placeholders e perfis exatamente como em produção, mas traria o framework para dentro de uma CLI
  que existe para não depender dele. A leitura em texto acerta os casos que as katas precisam.
- **Acusar `ddl-auto=update`.** O `update` não apaga dados. É uma prática ruim em produção
  (não remove colunas, não migra dados, não tem histórico), mas não é um erro que destrói. Fica para
  uma kata própria, se o corpus pedir.
- **Achar segredos pelo valor** (entropia alta, formato de chave da AWS), como fazem o gitleaks e o
  trufflehog. Pede outro tipo de ferramenta e erra mais. Pela chave, o Shishō acha o caso comum: uma
  configuração que diz o que é.
- **Saber qual perfil é a produção no Spring**, por exemplo tratando `prod` como produção. Um
  `application.properties` com `create-drop` e um `application-prod.properties` com `validate`
  continuaria sendo acusado. O nome do perfil é uma convenção do projeto, e adivinhá-la criaria uma
  regra que vale para uns e erra para outros.

## 📚 Estudo — configuração é código

O *Twelve-Factor App* diz que a configuração (o que muda entre ambientes) deve morar no ambiente, e
não no código. O `application.properties` fica no meio do caminho: está no repositório, como o
código, mas descreve o ambiente, como a configuração. Isso confunde, porque parece "só
configuração" e por isso não passa pela revisão com o mesmo cuidado.

Na prática, o arquivo tem três tipos de coisa:

1. **O que é igual em todo ambiente** (o nome da aplicação, o `open-in-view=false`). Pode ficar no
   arquivo, versionado, revisado como código.
2. **O que muda por ambiente, mas não é segredo** (a URL do banco, o nível de log). Fica no arquivo
   como placeholder com um padrão de desenvolvimento: `${DB_URL:jdbc:postgresql://localhost/loja}`.
3. **Segredos** (senhas, tokens, chaves). Nunca no arquivo. Só o placeholder, `${DB_PASSWORD}`, e o
   valor vem do ambiente: uma variável, um Secret do Kubernetes, um cofre.

Os perfis (`application-dev.yml`, `%dev.`) existem para o primeiro e o segundo tipo, não para o
terceiro. Um `application-prod.yml` com a senha de produção continua sendo uma senha no Git.

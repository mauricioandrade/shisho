# ADR 0022 — Desempenho em projetos grandes

- **Status:** aceito
- **Data:** 2026-10-18
- **Fase:** 21 (desempenho)

## Contexto

O hook do Claude Code ([ADR 0008](0008-claude-code-plugin.md)) roda uma análise a cada edição, e
um CI roda uma a cada push. O tempo de uma análise importa nos dois casos. Antes de mexer em
qualquer coisa, o Shishō foi medido nos projetos da calibração ([ADR 0016](0016-real-projects.md)).
Também entrou um projeto sintético de 4152 arquivos e 680 mil linhas: o código do `macrozheng/mall`
copiado oito vezes, para ver como o tempo cresce com o tamanho.

O tempo crescia de forma linear, cerca de 6 ms por arquivo, e não havia nada quadrático no caminho
principal. Um perfil com o JFR mostrou para onde ia o tempo:

| Onde | Parcela | Causa |
|---|---|---|
| `request-body-without-valid` | ~25% (sintético) | procurava restrições de validação na hierarquia da classe de **todo parâmetro de todo método**, e só depois olhava se ele tinha `@RequestBody` |
| parse do JavaParser | ~75% | um arquivo por vez, na thread `main` |
| `StrategyDetector` (shoshin) | pouco, mas quadrático | contava as implementações de cada interface percorrendo todas as classes |

A extração do modelo (`ClassUnit`) a partir da árvore era só 5% do parse. O resto é o próprio
JavaParser, a uns 50 mil linhas por segundo, que é o ritmo normal dele.

## Decisão

1. A `request-body-without-valid` olha primeiro a anotação, que é barata, e só procura restrições
   num parâmetro de corpo.
2. Os arquivos são parseados em paralelo, num `parallelStream`. A `JavaParser` não é thread-safe e
   custa caro criar uma, porque ela reaproveita o parser gerado entre as chamadas. Por isso cada
   thread guarda uma num `ThreadLocal`. Na primeira versão, que criava uma por arquivo, gastava-se o
   dobro de CPU.
3. O `StrategyDetector` conta as implementações por nome de supertipo uma vez só.

O resultado é o mesmo, byte a byte, nos relatórios de terminal, HTML, SARIF e shoshin dos dez
projetos. Um `toList()` sobre um stream paralelo mantém a ordem de encontro, e a lista de arquivos
já vinha ordenada pelo caminho. Um teste parseia 200 arquivos três vezes e confere a ordem.

| Projeto | Arquivos | Antes | Depois |
|---|---|---|---|
| sintético (mall ×8) | 4152 | 26,5 s | 12,6 s |
| `macrozheng/mall` | 519 | 5,2 s | 3,9 s |
| `quarkus-quickstarts` | 386 | 2,4 s | 1,8 s |
| o próprio Shishō | 204 | 2,2 s | 1,6 s |
| `spring-petclinic` | 30 | 1,2 s | 1,1 s |

Melhor de três rodadas, num Ryzen 5 3600 (6 núcleos, 12 threads) no WSL. O paralelismo satura
cedo: com duas threads o parse já cai quase à metade, e com doze ganha pouco mais. O JavaParser
aloca muito (um objeto por token), e a partir daí o limite é a memória, não a CPU.

## Alternativas registradas

- **Não guardar os tokens** (`setStoreTokens(false)`), para alocar menos. O parse passou a falhar:
  a atribuição de comentários, que a `swallowed-exception` usa, depende deles.
- **Cache do AOT do JDK 25** (`-XX:AOTCache`, JEPs 483 e 515), que guarda classes carregadas e
  perfis de métodos entre execuções e encurtaria a partida da JVM, que é quase todo o segundo que
  sobra num projeto pequeno. O cache só vale para a mesma versão exata do JDK, e o `install.sh`
  teria de gerá-lo na máquina de cada um. Fica para quando a partida for o problema medido.
- **Análise incremental no hook**, parseando só o arquivo editado. Pediria guardar o modelo entre
  execuções e saber o que cada kata de projeto lê de outras classes. Com 1–2 s num projeto médio, o
  ganho não paga essa complexidade hoje.
- **Um pool próprio de threads** em vez do pool comum do `ForkJoinPool`. O Shishō é uma CLI, e
  nada mais disputa o pool comum.

## 📚 Estudo — meça antes de otimizar

A frase do Knuth, "otimização prematura é a raiz de todo mal", quase sempre é citada pela metade. A
frase inteira diz que não se deve perder tempo com os 97% do código que não importam, **mas também
não deixar passar os 3% que importam**. O que separa um do outro é a medição.

Aqui, o palpite natural seria "o JavaParser é lento", e estaria certo só em parte. O perfil mostrou
um quarto do tempo numa kata, gasto num trabalho que ela depois jogava fora. Um filtro na ordem
errada é invisível na leitura do código, porque o resultado é o mesmo. Só aparece no perfil.

O **JFR** (JDK Flight Recorder) vem no JDK e custa quase nada para ligar:

```bash
java -XX:StartFlightRecording=filename=perfil.jfr,settings=profile -jar shisho.jar analyze .
jfr print --events jdk.ExecutionSample perfil.jfr
```

Cada amostra é uma pilha de chamadas tirada a cada poucos milissegundos. O método que aparece em
mais amostras é onde o tempo vai. Dois cuidados que este ADR aprendeu na prática. Primeiro, o JFR
guarda 64 frames por pilha, e um parser recursivo passa disso; para ver quem chamou, é preciso
`-XX:FlightRecorderOptions:stackdepth=2048`. Segundo, ele amostra só algumas threads por vez, então
não serve para medir o paralelismo. Para isso, compare o tempo de parede com o de CPU (`time`): se a
CPU sobe muito mais do que o tempo cai, há desperdício.

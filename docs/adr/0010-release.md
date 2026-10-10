# ADR 0010 — Release com o jar pronto

- **Status:** aceito
- **Data:** 2026-10-09
- **Fase:** 11 (release)

## Contexto

Até aqui, instalar o Shishō era clonar o repositório e compilar: JDK 25, Maven 3.9 e alguns
minutos de `mvn verify`. Para quem só quer analisar um projeto, é uma barreira desnecessária, e
o plugin do Claude Code ainda precisava que a pessoa dissesse onde o jar foi parar.

## Decisão

### 1. Uma GitHub Release por tag

O workflow `release.yml` roda a cada tag `v*` (por exemplo, `v0.1.0`):

1. compila com a versão da tag (`mvn -Drevision=0.1.0 verify`) e roda todos os testes;
2. confere que o jar responde `--version` com essa versão;
3. publica a release com o `shisho.jar` e o `shisho.jar.sha256`, com as notas geradas a partir
   dos commits.

A versão do pom passa a ser `${revision}`, o recurso do Maven para versões definidas no build.
No dia a dia ela continua `0.1.0-SNAPSHOT`, e só o workflow passa outra. O `--version` lê a
versão do `MANIFEST` do jar, então mostra a da release sem mudança de código.

A publicação usa o `gh`, que já vem nos runners do GitHub, com o token do próprio workflow: nada
de action de terceiros com permissão de escrita no repositório.

### 2. O `install.sh` baixa o jar

Por padrão, o instalador baixa o jar da última release e confere o SHA-256. Só precisa do
Java 25 para rodar o jar. `--from-source` mantém o caminho antigo, para quem está mexendo no
código.

Nos dois casos, o jar vai para `~/.local/share/shisho/shisho.jar`, e um executável
`~/.local/bin/shisho` o chama. Antes era um alias, mas um alias só existe no terminal
interativo: o Bash do Claude Code, os scripts e o CI não o enxergam. O bloco no `~/.bashrc` ou
no `~/.zshrc` passa a ter só o `PATH` e o `SHISHO_IDIOMA`.

O instalador também funciona sem clonar nada:

```bash
bash -c "$(curl -fsSL https://raw.githubusercontent.com/mauricioandrade/shiso/main/install.sh)"
```

### 3. O plugin acha o jar sozinho

O caminho do jar no plugin do Claude Code passa a ter como padrão o lugar onde o instalador o
põe. Quem usou o `install.sh` não precisa configurar nada.

## Alternativas registradas

- **Maven Central.** Faz sentido para uma biblioteca, e o Shishō é uma ferramenta: ninguém
  depende dele no `pom.xml`. Exigiria assinatura GPG e uma conta no Sonatype por um ganho que
  não existe hoje.
- **Imagem nativa com GraalVM.** Dispensaria o Java para rodar e começaria mais rápido, mas o
  Shishō lê o catálogo de dentro do jar e usa reflexão no picocli, e cada sistema operacional
  pediria um binário. Fica para quando o tempo de início for um problema medido.
- **Homebrew, SDKMAN, Scoop.** Cada um é mais um lugar para manter. A release é a fonte; um
  pacote pode apontar para ela depois.
- **Alias em vez do executável.** Era o que existia, e só funciona no shell interativo.

## Consequências

- Publicar uma versão é `git tag v0.1.0 && git push origin v0.1.0`.
- O `install.sh` precisa do `curl` e, para conferir o jar, do `sha256sum` ou do `shasum`.
- A GitHub Action (`action.yml`) continua compilando a partir do código: é o mesmo código do
  commit que está sendo analisado.

## 📚 Estudo — o checksum do download

O `shisho.jar.sha256` é a impressão digital do jar. O instalador calcula a do arquivo que baixou
e compara: se um único byte mudou no caminho (um download cortado, um proxy que alterou o
arquivo), as duas não batem e o jar não é instalado. O checksum não substitui uma assinatura:
quem controla a release controla os dois arquivos. Mas ele pega o erro mais comum, que é o
arquivo chegar diferente do que foi publicado.

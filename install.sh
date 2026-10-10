#!/usr/bin/env bash
# Builds the Shishō and sets up the `shisho` command, asking which language it should speak.
#   ./install.sh          asks the language
#   ./install.sh en       English, no questions (pt for Portuguese)
# See docs/adr/0007-languages.md.
set -euo pipefail

cd "$(dirname "$0")"
root="$(pwd)"
begin="# >>> shisho >>>"
end="# <<< shisho <<<"

if ! command -v mvn >/dev/null 2>&1; then
    echo "Maven não encontrado / Maven not found: https://maven.apache.org (ou/or: mise install)" >&2
    exit 1
fi
java_version="$(java -version 2>&1 | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -1)"
if [ -z "$java_version" ] || [ "$java_version" -lt 25 ]; then
    echo "O Shishō precisa do JDK 25 / The Shishō needs JDK 25 (ou/or: mise install)" >&2
    exit 1
fi

language="${1:-}"
while [ "$language" != "pt" ] && [ "$language" != "en" ]; do
    read -r -p "Idioma / Language [pt/en] (pt): " language
    language="${language:-pt}"
done

if [ "$language" = "pt" ]; then
    echo "Compilando o Shishō..."
else
    echo "Building the Shishō..."
fi
mvn -q -DskipTests package

case "$(basename "${SHELL:-bash}")" in
    zsh) rc="$HOME/.zshrc" ;;
    *) rc="$HOME/.bashrc" ;;
esac
block="$begin
alias shisho='java -jar \"$root/target/shisho.jar\"'
export SHISHO_IDIOMA=$language
$end"

if [ "$language" = "pt" ]; then
    read -r -p "Adicionar o comando shisho ao $rc? [S/n] " answer
else
    read -r -p "Add the shisho command to $rc? [Y/n] " answer
fi
case "${answer:-y}" in
    [nN]*)
        echo
        echo "$block"
        exit 0
        ;;
esac

touch "$rc"
# Replaces a previous installation's block instead of adding a second one.
sed -i.bak "/^$begin\$/,/^$end\$/d" "$rc" && rm -f "$rc.bak"
if [ -n "$(tail -n 1 "$rc")" ]; then
    printf '\n' >> "$rc"
fi
printf '%s\n' "$block" >> "$rc"

if [ "$language" = "pt" ]; then
    echo "Pronto. Abra um terminal novo (ou rode: source $rc) e experimente: shisho analyze <projeto>"
    echo "No Claude Code: /plugin install shisho --marketplace mauricioandrade/shiso"
    echo "  e, quando ele pedir o shisho.jar: $root/target/shisho.jar"
else
    echo "Done. Open a new terminal (or run: source $rc) and try: shisho analyze <project>"
    echo "In Claude Code: /plugin install shisho --marketplace mauricioandrade/shiso"
    echo "  and, when it asks for shisho.jar: $root/target/shisho.jar"
fi

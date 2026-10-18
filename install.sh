#!/usr/bin/env bash
# Installs the Shishō and asks which language it should speak. See docs/adr/0010-release.md.
#   ./install.sh                  downloads shisho.jar from the latest GitHub Release
#   ./install.sh --from-source    builds it from this clone instead (needs Maven)
#   ./install.sh en               English, no questions (pt for Portuguese)
# Without cloning:
#   bash -c "$(curl -fsSL https://raw.githubusercontent.com/mauricioandrade/shisho/main/install.sh)"
set -euo pipefail

release_url="${SHISHO_RELEASE_URL:-https://github.com/mauricioandrade/shisho/releases/latest/download}"
home_dir="${SHISHO_HOME:-$HOME/.local/share/shisho}"
bin_dir="$HOME/.local/bin"
begin="# >>> shisho >>>"
end="# <<< shisho <<<"

language=""
from_source=false
for argument in "$@"; do
    case "$argument" in
        pt | en) language="$argument" ;;
        --from-source) from_source=true ;;
        *)
            echo "Uso / usage: install.sh [pt|en] [--from-source]" >&2
            exit 2
            ;;
    esac
done

java_version="$(java -version 2>&1 | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -1 || true)"
if [ -z "$java_version" ] || [ "$java_version" -lt 25 ]; then
    echo "O Shishō precisa do Java 25 / The Shishō needs Java 25: https://adoptium.net" >&2
    exit 1
fi

# Without a terminal (a script, CI), read hits the end of input: the defaults apply, as if
# the person pressed Enter, instead of set -e ending the script without a word.
while [ "$language" != "pt" ] && [ "$language" != "en" ]; do
    read -r -p "Idioma / Language [pt/en] (pt): " language || echo
    language="${language:-pt}"
done
say() {
    if [ "$language" = "pt" ]; then echo "$1"; else echo "$2"; fi
}

mkdir -p "$home_dir" "$bin_dir"
if [ "$from_source" = true ]; then
    root="$(cd "$(dirname "$0")" && pwd)"
    if [ ! -f "$root/pom.xml" ] || ! command -v mvn >/dev/null 2>&1; then
        say "--from-source precisa do clone do repositório e do Maven." "--from-source needs a clone of the repository and Maven." >&2
        exit 1
    fi
    say "Compilando o Shishō..." "Building the Shishō..."
    (cd "$root" && mvn -q -DskipTests package)
    cp "$root/target/shisho.jar" "$home_dir/shisho.jar"
else
    say "Baixando o Shishō..." "Downloading the Shishō..."
    download="$(mktemp -d)"
    trap 'rm -rf "${download:?}"' EXIT
    curl -fsSL "$release_url/shisho.jar" -o "$download/shisho.jar"
    curl -fsSL "$release_url/shisho.jar.sha256" -o "$download/shisho.jar.sha256"
    expected="$(cut -d ' ' -f 1 "$download/shisho.jar.sha256")"
    if command -v sha256sum >/dev/null 2>&1; then
        actual="$(sha256sum "$download/shisho.jar" | cut -d ' ' -f 1)"
    else
        actual="$(shasum -a 256 "$download/shisho.jar" | cut -d ' ' -f 1)"
    fi
    if [ "$expected" != "$actual" ]; then
        say "O jar baixado não confere com o checksum da release. Nada foi instalado." \
            "The downloaded jar does not match the release checksum. Nothing was installed." >&2
        exit 1
    fi
    mv "$download/shisho.jar" "$home_dir/shisho.jar"
fi

# An executable, not an alias: scripts, CI and Claude Code's Bash tool see it too.
cat > "$bin_dir/shisho" <<LAUNCHER
#!/bin/sh
exec java -jar "$home_dir/shisho.jar" "\$@"
LAUNCHER
chmod +x "$bin_dir/shisho"

case "$(basename "${SHELL:-bash}")" in
    zsh) rc="$HOME/.zshrc" ;;
    *) rc="$HOME/.bashrc" ;;
esac
block="$begin
export PATH=\"$bin_dir:\$PATH\"
export SHISHO_IDIOMA=$language
$end"

if [ "$language" = "pt" ]; then
    read -r -p "Adicionar o shisho ao PATH e o idioma ao $rc? [S/n] " answer || echo
else
    read -r -p "Add shisho to the PATH and the language to $rc? [Y/n] " answer || echo
fi
case "${answer:-y}" in
    [nN]*)
        echo
        echo "$block"
        ;;
    *)
        touch "$rc"
        # Replaces a previous installation's block instead of adding a second one.
        sed -i.bak "/^$begin\$/,/^$end\$/d" "$rc" && rm -f "$rc.bak"
        if [ -n "$(tail -n 1 "$rc")" ]; then
            printf '\n' >> "$rc"
        fi
        printf '%s\n' "$block" >> "$rc"
        ;;
esac

echo
say "Pronto: $("$bin_dir/shisho" --version). Abra um terminal novo (ou rode: source $rc) e experimente: shisho analyze <projeto>" \
    "Done: $("$bin_dir/shisho" --version). Open a new terminal (or run: source $rc) and try: shisho analyze <project>"
say "No Claude Code: /plugin install shisho --marketplace mauricioandrade/shisho" \
    "In Claude Code: /plugin install shisho --marketplace mauricioandrade/shisho"

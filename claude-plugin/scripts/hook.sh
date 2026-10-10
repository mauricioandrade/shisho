#!/bin/sh
# PostToolUse hook: hands the hook's JSON to `shisho claude-hook`, which shows Claude the lessons
# of the file it just edited. See docs/adr/0008-claude-code-plugin.md.
jar="${CLAUDE_PLUGIN_OPTION_SHISHO_JAR:-${SHISHO_JAR:-$HOME/.local/share/shisho/shisho.jar}}"
case "$jar" in
    "~/"*) jar="$HOME/${jar#\~/}" ;;
esac
if [ ! -f "$jar" ]; then
    echo "Shishō: set the shisho_jar option of the shisho plugin (/config). Not found: ${jar:-<empty>}" >&2
    exit 1
fi
exec java -jar "$jar" claude-hook

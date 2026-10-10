#!/usr/bin/env bash
# Records the README demos, docs/demo/demo.gif (Portuguese) and docs/demo/demo-en.gif (English),
# with VHS in Docker. Needs Docker and a Linux JDK 25 on the PATH, which is mounted into the
# container. The image adds CJK fonts, so 師匠 renders.
set -euo pipefail
cd "$(dirname "$0")/../.."

mvn -q -DskipTests package
docker build -q -t shisho-vhs docs/demo >/dev/null
jdk="$(dirname "$(dirname "$(readlink -f "$(command -v java)")")")"

for language in pt en; do
    output="docs/demo/demo.gif"
    [ "$language" = en ] && output="docs/demo/demo-en.gif"
    tape="$(mktemp --suffix .tape)"
    sed "s|^Output .*|Output $output|" docs/demo/demo.tape > "$tape"
    docker run --rm \
        --user "$(id -u):$(id -g)" -e HOME=/tmp \
        -v "$PWD":/repo -w /repo -v "$tape":/demo.tape:ro -v "$jdk":/opt/jdk:ro \
        -e SHISHO_IDIOMA="$language" -e LANG=C.UTF-8 -e LC_ALL=C.UTF-8 \
        -e PATH=/opt/jdk/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin \
        shisho-vhs /demo.tape
    rm -f "$tape"
    echo "$output"
done

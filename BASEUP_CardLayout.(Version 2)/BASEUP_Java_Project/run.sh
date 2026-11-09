#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p out
find src -name '*.java' > .sources.txt
javac --release 17 -encoding UTF-8 -d out @.sources.txt
rm -f .sources.txt
java -cp out baseup.Main

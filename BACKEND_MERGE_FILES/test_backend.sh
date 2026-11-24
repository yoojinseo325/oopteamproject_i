#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p build/classes
find src/main/java src/test/java -name '*.java' -print0 | xargs -0 javac --release 17 -encoding UTF-8 -d build/classes
java -cp build/classes baseup.shortapp.MixedBackendTest data/short_questions.tsv
java -cp build/classes:resources baseup.shortapp.MigrationTest data/short_questions.tsv

#!/usr/bin/env bash
# SACCOLink GUI - compile script
# Place the Oracle JDBC driver (ojdbc11.jar) in src/lib/ before compiling.
set -euo pipefail
cd "$(dirname "$0")"

mkdir -p out

JARS=$(find lib -name '*.jar' 2>/dev/null || true)
CP="out"
if [ -n "$JARS" ]; then
    CP="$CP:$(echo "$JARS" | tr '\n' ':')"
fi

echo "Compiling with classpath: $CP"
find saccolink -name '*.java' > sources.txt
javac -encoding UTF-8 -d out @sources.txt
rm -f sources.txt

echo "Build OK. Run with: ./run.sh"

#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
python3 scripts/build.py
source scripts/java8.sh
mkdir -p .cache/buildtools server/plugins
if [[ ! -f server/server.jar ]]; then
  echo 'Compilando Spigot 1.8.8 com BuildTools oficial. A primeira execução pode demorar.'
  curl --fail --location --retry 3 'https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar' -o .cache/buildtools/BuildTools.jar
  (
    cd .cache/buildtools
    export JAVA_HOME
    JAVA_HOME="$(cd -- "$(dirname -- "$JAVA8_BIN")/.." && pwd)"
    export PATH="$JAVA_HOME/bin:$PATH"
    "$JAVA8_BIN" -jar BuildTools.jar --rev 1.8.8 --compile SPIGOT
  )
  cp .cache/buildtools/spigot-1.8.8.jar server/server.jar
fi
cp target/TreinoHub-1.0.0.jar server/plugins/
if [[ ! -f server/server.properties ]]; then cp config/server.properties server/; fi
if [[ ! -f server/eula.txt ]]; then printf 'eula=false\n' > server/eula.txt; fi
printf '\nInstalado. Leia README.md antes de iniciar.\n'

#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
bash scripts/bootstrap.sh
source scripts/env.sh
JAVA_RUN="$(command -v java)"
if [[ ! -f server/.bwfe-template.json ]]; then
  source scripts/java8.sh
  JAVA_RUN="$JAVA8_BIN"
else
  python3 -c 'import sys;sys.path.insert(0,"scripts");import template;from pathlib import Path;r=template.report(Path("server"));sys.exit("Template incompleto: execute bash atualizar.sh" if r["mapas_ausentes"] or r["plugins_ausentes"] else 0)'
fi
python3 scripts/playit.py check
[[ -f server/server.jar && ( -f server/.bwfe-template.json || -f server/plugins/TreinoHub-1.0.0.jar ) ]] || { echo 'Execute bash scripts/install.sh primeiro.'; exit 1; }
python3 scripts/server_config.py check
if [[ "${ACCEPT_EULA:-false}" == true ]]; then printf 'eula=true\n' > server/eula.txt; fi
if ! grep -q '^eula=true$' server/eula.txt; then
  echo 'Leia https://aka.ms/MinecraftEULA . Se concordar, execute: ACCEPT_EULA=true bash scripts/start.sh'
  exit 1
fi
if [[ "${TREINO_DIRECT:-0}" != 1 ]]; then
  exec python3 scripts/playit.py run
fi
cd server
exec "$JAVA_RUN" -Xms512M -Xmx"${SERVER_RAM:-2G}" -jar server.jar nogui

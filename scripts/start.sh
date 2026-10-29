#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
source scripts/java8.sh
python3 scripts/playit.py check
[[ -f server/server.jar && -f server/plugins/TreinoHub-1.0.0.jar ]] || { echo 'Execute bash scripts/install.sh primeiro.'; exit 1; }
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
exec "$JAVA8_BIN" -Xms512M -Xmx"${SERVER_RAM:-2G}" -jar server.jar nogui

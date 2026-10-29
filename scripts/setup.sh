#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
printf '\nRYUEX TREINO — 1.8.9\nPrimeiro Playit, depois instalação do servidor.\n\n'
python3 scripts/playit.py setup
bash scripts/install.sh
printf '\nSetup concluído. Leia docs/GUIA.md para aceitar a EULA e iniciar.\n'

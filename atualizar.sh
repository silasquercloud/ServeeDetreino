#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"
if [[ "${1:-}" != --local ]]; then
  if [[ -n "$(git status --porcelain --untracked-files=no)" ]]; then
    echo 'Há alterações locais no código. Preserve-as no Git antes de atualizar.'; exit 1
  fi
  git pull --ff-only
  exec bash "$ROOT/atualizar.sh" --local "$@"
fi
shift
bash scripts/bootstrap.sh
source scripts/env.sh
exec python3 scripts/prepare-template.py "$@"

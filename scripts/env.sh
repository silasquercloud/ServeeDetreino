#!/usr/bin/env bash
# Source after bootstrap.sh; changes only this launcher and its child processes.
TREINO_ENV_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
if [[ ! -f "$TREINO_ENV_ROOT/.toolchains/environment.sh" ]]; then
  echo 'Ambiente ausente. Execute bash scripts/bootstrap.sh.' >&2
  return 1
fi
source "$TREINO_ENV_ROOT/.toolchains/environment.sh"

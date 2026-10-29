#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
bash "$ROOT/scripts/bootstrap.sh"
source "$ROOT/scripts/env.sh"
exec python3 "$ROOT/scripts/menu.py"

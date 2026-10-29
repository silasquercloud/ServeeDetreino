#!/usr/bin/env bash
# Compatibility entry point for the interactive installer menu.
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
exec python3 "$ROOT/scripts/menu.py"

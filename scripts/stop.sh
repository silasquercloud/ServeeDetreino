#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
# Stopping must not download tools, contact Playit, or install packages.
command -v python3 >/dev/null || { echo 'Python ausente. Digite stop no console original.'; exit 1; }
exec python3 "$ROOT/scripts/server_control.py"

#!/usr/bin/env bash
# This entry point MUST work before Python is installed.
set -euo pipefail
TREINO_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"

base_ready() {
  local tool
  for tool in python3 git curl tar gzip unzip zip; do
    command -v "$tool" >/dev/null 2>&1 || return 1
  done
  [[ -s /etc/ssl/certs/ca-certificates.crt ]] || return 1
  python3 -c 'import ssl, venv, pip, PIL' >/dev/null 2>&1 || return 1
}

if ! base_ready; then
  printf '\nPreparando dependências do sistema (Python, Git, curl, certificados e utilitários)...\n'
  TREINO_PRIVILEGE=()
  if [[ "$(id -u)" != 0 ]]; then
    if ! command -v sudo >/dev/null 2>&1; then
      echo 'É necessário root ou sudo para instalar os pacotes ausentes.' >&2
      exit 1
    fi
    TREINO_PRIVILEGE=(sudo)
  fi
  if command -v apt-get >/dev/null 2>&1; then
    "${TREINO_PRIVILEGE[@]}" apt-get update
    "${TREINO_PRIVILEGE[@]}" env DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends python3 python3-venv python3-pip python3-pil git curl ca-certificates tar gzip unzip zip libstdc++6 zlib1g libfreetype6 fontconfig fonts-dejavu-core
  elif command -v pacman >/dev/null 2>&1; then
    "${TREINO_PRIVILEGE[@]}" pacman -S --needed --noconfirm python python-pip python-pillow git curl ca-certificates tar gzip unzip zip gcc-libs zlib freetype2 fontconfig ttf-dejavu
  else
    echo 'Instalação automática disponível em Ubuntu/Debian e Arch/CachyOS. Gerenciador de pacotes não encontrado.' >&2
    exit 1
  fi
  base_ready || { echo 'A instalação das dependências básicas não foi concluída.' >&2; exit 1; }
fi

# Useful for diagnosing only the system packages, without downloading Java/Playit.
if [[ "${1:-}" == --base-only ]]; then exit 0; fi
python3 "$TREINO_ROOT/scripts/dependencies.py"

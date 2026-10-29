#!/usr/bin/env bash
# Carregado com source. Pode-se indicar JAVA8_BIN=/caminho/java.
set -euo pipefail
TREINO_JAVA_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
if [[ -z "${JAVA8_BIN:-}" && -x "$TREINO_JAVA_ROOT/.toolchains/java8/bin/java" ]]; then
  JAVA8_BIN="$TREINO_JAVA_ROOT/.toolchains/java8/bin/java"
fi
if [[ -z "${JAVA8_BIN:-}" ]]; then
  for candidate in /usr/local/sdkman/candidates/java/8*/bin/java "$HOME"/.sdkman/candidates/java/8*/bin/java; do
    if [[ -x "$candidate" ]]; then JAVA8_BIN="$candidate"; break; fi
  done
fi
JAVA8_BIN="${JAVA8_BIN:-java}"
JAVA8_BIN="$(readlink -f -- "$(command -v -- "$JAVA8_BIN")")"
if ! "$JAVA8_BIN" -version 2>&1 | head -1 | grep -q '"1\.8\.'; then
  echo 'É necessário Java 8. Configure JAVA8_BIN=/caminho/do/java8/bin/java.' >&2
  exit 1
fi

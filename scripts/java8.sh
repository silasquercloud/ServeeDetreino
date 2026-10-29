#!/usr/bin/env bash
# Carregado com source. Pode-se indicar JAVA8_BIN=/caminho/java.
set -euo pipefail
if [[ -z "${JAVA8_BIN:-}" ]]; then
  for candidate in /usr/local/sdkman/candidates/java/8*/bin/java "$HOME"/.sdkman/candidates/java/8*/bin/java; do
    if [[ -x "$candidate" ]]; then JAVA8_BIN="$candidate"; break; fi
  done
fi
JAVA8_BIN="${JAVA8_BIN:-java}"
if ! "$JAVA8_BIN" -version 2>&1 | head -1 | grep -q '"1\.8\.'; then
  echo 'É necessário Java 8. Configure JAVA8_BIN=/caminho/do/java8/bin/java.' >&2
  exit 1
fi

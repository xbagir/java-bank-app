#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if [ -f .env ]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

JAVA_BIN="${JAVA_BIN:-java}"

start() {
  local module="$1"
  local jar
  jar="$(find "$module/target" -maxdepth 1 -name "${module}-*-SNAPSHOT.jar" ! -name "*-stubs.jar" | head -1)"
  if [ -z "$jar" ]; then
    echo "JAR для модуля $module не найден. Сначала выполните ./mvnw clean package" >&2
    exit 1
  fi
  echo "Starting $module ($jar)"
  "$JAVA_BIN" -jar "$jar" &
  PIDS+=("$!")
}

PIDS=()
start discovery-server
start config-server
sleep 5
start notifications
start accounts
start cash
start transfer
start gateway
start front

trap 'kill "${PIDS[@]}" 2>/dev/null || true' EXIT INT TERM
wait

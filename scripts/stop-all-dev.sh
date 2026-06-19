#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PID_DIR="${ROOT_DIR}/.dev-pids"

stop_service() {
  local name="$1"
  local pid_file="${PID_DIR}/${name}.pid"

  if [ ! -f "${pid_file}" ]; then
    echo "${name} not running: missing pid file"
    return 0
  fi

  local pid
  pid="$(cat "${pid_file}")"
  if [ -z "${pid}" ]; then
    rm -f "${pid_file}"
    echo "${name} not running: empty pid file"
    return 0
  fi

  if kill -0 "${pid}" >/dev/null 2>&1; then
    echo "Stopping ${name}: pid ${pid}"
    kill "${pid}"
  else
    echo "${name} already stopped: pid ${pid}"
  fi

  rm -f "${pid_file}"
}

stop_service "bot"
stop_service "frontend"
stop_service "backend"

echo "Dev services stopped."

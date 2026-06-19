#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PID_DIR="${ROOT_DIR}/.dev-pids"
LOG_DIR="${ROOT_DIR}/.dev-logs"

mkdir -p "${PID_DIR}" "${LOG_DIR}"

start_service() {
  local name="$1"
  local script="$2"
  local pid_file="${PID_DIR}/${name}.pid"
  local log_file="${LOG_DIR}/${name}.log"

  if [ -f "${pid_file}" ]; then
    local old_pid
    old_pid="$(cat "${pid_file}")"
    if [ -n "${old_pid}" ] && kill -0 "${old_pid}" >/dev/null 2>&1; then
      echo "${name} already running: pid ${old_pid}"
      return 0
    fi
    rm -f "${pid_file}"
  fi

  echo "Starting ${name}, log: ${log_file}"
  (
    cd "${ROOT_DIR}"
    nohup "${script}" >"${log_file}" 2>&1 </dev/null &
    echo "$!" >"${pid_file}"
  )
}

start_service "backend" "${ROOT_DIR}/scripts/start-backend-dev.sh"
start_service "frontend" "${ROOT_DIR}/scripts/start-frontend-dev.sh"
start_service "bot" "${ROOT_DIR}/scripts/start-bot-dev.sh"

echo "All dev services are starting. Logs are in ${LOG_DIR}."
echo "Stop them with: ./scripts/stop-all-dev.sh"

#!/usr/bin/env bash
set -euo pipefail

FRONTEND_HOST="${FRONTEND_HOST:-127.0.0.1}"
FRONTEND_PORT="${FRONTEND_PORT:-5173}"

exec npm --prefix frontend run dev -- --host "${FRONTEND_HOST}" --port "${FRONTEND_PORT}"

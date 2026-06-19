#!/usr/bin/env bash
set -euo pipefail

BOT_HOST="${BOT_HOST:-127.0.0.1}"
BOT_PORT="${BOT_PORT:-9000}"
BOT_DIR="${BOT_DIR:-cs-bot-python}"

if [ ! -d "${BOT_DIR}" ]; then
  echo "Bot directory not found: ${BOT_DIR}" >&2
  exit 1
fi

cd "${BOT_DIR}"

if [ ! -x ".venv/bin/uvicorn" ]; then
  echo "Missing bot virtualenv. Run:" >&2
  echo "  cd ${BOT_DIR} && python3 -m venv .venv && source .venv/bin/activate && pip install -r requirements.txt" >&2
  exit 1
fi

if [ ! -f ".env" ]; then
  echo "Missing ${BOT_DIR}/.env. Run:" >&2
  echo "  cp ${BOT_DIR}/.env.example ${BOT_DIR}/.env" >&2
  echo "Then configure DEEPSEEK_API_KEY." >&2
  exit 1
fi

exec .venv/bin/uvicorn app.main:app --host "${BOT_HOST}" --port "${BOT_PORT}"

#!/usr/bin/env bash
set -euo pipefail

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-root}"
DB_NAME="${DB_NAME:-hmdp}"

if [[ ! "${DB_NAME}" =~ ^[A-Za-z0-9_]+$ ]]; then
  echo "Invalid DB_NAME: only letters, numbers and underscores are allowed" >&2
  exit 1
fi

mysql -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}" -e "CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\` DEFAULT CHARACTER SET utf8mb4;"
mysql -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}" \
  --init-command="SET SESSION sql_mode='NO_ENGINE_SUBSTITUTION';" \
  "${DB_NAME}" < src/main/resources/db/hmdp.sql

mysql -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}" \
  "${DB_NAME}" < src/main/resources/db/hmdp_admin_compat.sql

mysql -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}" \
  "${DB_NAME}" < src/main/resources/db/hmdp_perf_indexes.sql

mysql -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}" \
  "${DB_NAME}" < src/main/resources/db/hmdp_demo_seed.sql

echo "Database initialized: ${DB_NAME}"

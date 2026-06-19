#!/usr/bin/env bash
set -euo pipefail

DEFAULT_JAVA_HOME="/opt/homebrew/Cellar/openjdk@11/11.0.31/libexec/openjdk.jdk/Contents/Home"
if [ -z "${JAVA_HOME:-}" ] && [ -d "${DEFAULT_JAVA_HOME}" ]; then
  export JAVA_HOME="${DEFAULT_JAVA_HOME}"
fi

if [ -n "${JAVA_HOME:-}" ]; then
  export PATH="${JAVA_HOME}/bin:${PATH}"
fi

DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-}"
REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
RABBITMQ_HOST="${RABBITMQ_HOST:-127.0.0.1}"
RABBITMQ_PORT="${RABBITMQ_PORT:-5672}"
RABBITMQ_USER="${RABBITMQ_USER:-guest}"
RABBITMQ_PASSWORD="${RABBITMQ_PASSWORD:-guest}"
RABBITMQ_VHOST="${RABBITMQ_VHOST:-/}"

mvn -DskipTests spring-boot:run \
  -Dspring-boot.run.arguments="--spring.datasource.username=${DB_USER} --spring.datasource.password=${DB_PASSWORD} --spring.redis.host=${REDIS_HOST} --spring.rabbitmq.host=${RABBITMQ_HOST} --spring.rabbitmq.port=${RABBITMQ_PORT} --spring.rabbitmq.username=${RABBITMQ_USER} --spring.rabbitmq.password=${RABBITMQ_PASSWORD} --spring.rabbitmq.virtual-host=${RABBITMQ_VHOST}"

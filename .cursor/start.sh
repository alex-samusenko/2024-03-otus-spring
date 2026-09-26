#!/usr/bin/env bash
# Per-boot runtime initialization for the Cloud Agent environment.
# Starts PostgreSQL, ensures the moodle database/role exist, and launches the
# Spring Boot backend (:8080) and the Vite frontend dev server (:5173).
# Idempotent: safe to run on every container start; it never duplicates services.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_DIR="$REPO_ROOT/hw03-spring-boot"

port_open() {
  (exec 3<>"/dev/tcp/127.0.0.1/$1") 2>/dev/null && exec 3>&- && return 0
  return 1
}

# 1. PostgreSQL
if ! pg_isready -q -h localhost -p 5432; then
  sudo pg_ctlcluster 16 main start || true
fi
for _ in $(seq 1 30); do
  pg_isready -q -h localhost -p 5432 && break
  sleep 1
done

if ! sudo -u postgres psql -tAc "SELECT 1 FROM pg_roles WHERE rolname='moodle'" | grep -q 1; then
  sudo -u postgres psql -c "CREATE ROLE moodle LOGIN PASSWORD 'moodle';"
fi
if ! sudo -u postgres psql -tAc "SELECT 1 FROM pg_database WHERE datname='moodle'" | grep -q 1; then
  sudo -u postgres psql -c "CREATE DATABASE moodle OWNER moodle;"
fi

# 2. Backend (Spring Boot on :8080). Creates its schema and seeds users on boot.
if port_open 8080; then
  echo "Backend already listening on :8080"
else
  ( cd "$APP_DIR" && setsid nohup java -jar target/hw03-spring-boot-1.0.jar \
      > /tmp/backend.log 2>&1 < /dev/null & )
  echo "Started Spring Boot backend (logs: /tmp/backend.log)"
fi

# 3. Frontend (Vite dev server on :5173, proxies /api to the backend).
if port_open 5173; then
  echo "Frontend already listening on :5173"
else
  ( cd "$APP_DIR/frontend" && setsid nohup npm run dev -- --host \
      > /tmp/frontend.log 2>&1 < /dev/null & )
  echo "Started Vite frontend (logs: /tmp/frontend.log)"
fi

for _ in $(seq 1 60); do
  port_open 8080 && break
  sleep 1
done

echo "Environment ready: backend http://localhost:8080  frontend http://localhost:5173"

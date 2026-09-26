#!/usr/bin/env bash
# Per-boot runtime initialization for the Cloud Agent environment.
# Starts PostgreSQL and ensures the moodle database/role exist.
# The Spring Boot app creates its own schema and seeds users on startup.
set -euo pipefail

if ! pg_isready -q -h localhost -p 5432; then
  sudo pg_ctlcluster 16 main start || true
fi

for _ in $(seq 1 30); do
  if pg_isready -q -h localhost -p 5432; then
    break
  fi
  sleep 1
done

if ! sudo -u postgres psql -tAc "SELECT 1 FROM pg_roles WHERE rolname='moodle'" | grep -q 1; then
  sudo -u postgres psql -c "CREATE ROLE moodle LOGIN PASSWORD 'moodle';"
fi

if ! sudo -u postgres psql -tAc "SELECT 1 FROM pg_database WHERE datname='moodle'" | grep -q 1; then
  sudo -u postgres psql -c "CREATE DATABASE moodle OWNER moodle;"
fi

echo "PostgreSQL ready: moodle database available on localhost:5432"

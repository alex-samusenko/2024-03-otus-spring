#!/usr/bin/env bash
# Repository bootstrap for the Cloud Agent environment.
# Idempotent: builds the hw03-spring-boot backend jar and installs the frontend.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_DIR="$REPO_ROOT/hw03-spring-boot"

cd "$APP_DIR/frontend"
npm install
# Build the SPA into src/main/resources/static so the packaged jar serves it.
npm run build

cd "$APP_DIR"
# Package the runnable jar (tests run separately; skip here to keep bootstrap fast).
mvn -B -DskipTests clean package

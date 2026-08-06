#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

echo "[stop-app] Stopping all services..."
docker compose down

echo "[stop-app] All services stopped."
echo "  To also wipe data volumes: docker compose down -v"

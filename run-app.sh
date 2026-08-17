#!/usr/bin/env bash
set -euo pipefail

# ── Config ───────────────────────────────────────────────────────────────────
COMPOSE_FILE="docker-compose.yml"
TIMEOUT=120   # seconds to wait for transaction-service to be ready

# ── Helpers ──────────────────────────────────────────────────────────────────
log()  { echo "[run-app] $*"; }
fail() { echo "[run-app] ERROR: $*" >&2; exit 1; }

wait_for_url() {
  local url=$1 label=$2 elapsed=0
  log "Waiting for $label..."
  until curl -sf "$url" > /dev/null 2>&1; do
    sleep 3; elapsed=$((elapsed + 3))
    [[ $elapsed -ge $TIMEOUT ]] && fail "$label did not become ready in ${TIMEOUT}s"
  done
  log "$label is up."
}

# ── Main ─────────────────────────────────────────────────────────────────────
cd "$(dirname "$0")"

log "Building and starting all services..."
if docker compose -f "$COMPOSE_FILE" up --build -d; then
  initial_start_failed=false
else
  initial_start_failed=true
fi

# Restart Zookeeper if Kafka exited due to a stale ephemeral node (NodeExistsException)
if [[ "$(docker compose -f "$COMPOSE_FILE" ps --status exited -q kafka 2>/dev/null)" != "" ]]; then
  log "Kafka exited — clearing stale Zookeeper node and retrying..."
  docker compose -f "$COMPOSE_FILE" restart zookeeper
  # ZooKeeper restores sessions after a restart. Give the old broker session
  # enough time to expire before Kafka tries to register broker ID 1 again.
  sleep 20
  docker compose -f "$COMPOSE_FILE" up -d kafka kafka-ui transaction-service fraud-service frontend
elif [[ "$initial_start_failed" == true ]]; then
  fail "Docker Compose failed before the application services could start"
fi

# Wait for the two app services (infra health checks are handled by depends_on)
wait_for_url "http://localhost:8080/actuator/health" "transaction-service"
wait_for_url "http://localhost:8090/health"          "fraud-service"

echo ""
echo "============================================"
echo "  All services are up!"
echo "============================================"
echo "  Frontend:           http://localhost:3000"
echo "  Transaction API:    http://localhost:8080"
echo "  Fraud API:          http://localhost:8090"
echo "  Kafka UI:           http://localhost:8082"
echo "  Adminer (Postgres): http://localhost:8888"
echo "  Mongo Express:      http://localhost:8081"
echo "============================================"
echo ""
echo "  To stop:   docker compose down"
echo "  To reset:  docker compose down -v   (wipes volumes)"
echo ""

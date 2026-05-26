#!/bin/bash
# Deploy skripta — sproži se preko webhook-a ob vsakem uspešnem GitHub Actions build-u.
# Argumenti (opcijski): $1 = commit SHA, $2 = git ref (npr. refs/heads/develop)

set -euo pipefail

COMMIT="${1:-unknown}"
REF="${2:-unknown}"
PROJECT_DIR="/home/VirtualEstate/VirtualEstate_Project"
COMPOSE_FILE="docker-compose.yml"
LOG_PREFIX="[deploy $(date -u +%Y-%m-%dT%H:%M:%SZ)]"

echo "$LOG_PREFIX Začetek (commit=$COMMIT ref=$REF)"

cd "$PROJECT_DIR"

# 1) Povleci najnovejšo verzijo izvorne kode (zaradi docker-compose.yml + deploy/ datotek)
echo "$LOG_PREFIX git pull..."
git pull --rebase --autostash

# 2) Prenesi najnovejše docker slike iz Docker Hub
echo "$LOG_PREFIX docker compose pull..."
docker compose -f "$COMPOSE_FILE" pull

# 3) Ustavi obstoječe kontejnerje in zaženi nove
echo "$LOG_PREFIX docker compose up -d..."
docker compose -f "$COMPOSE_FILE" up -d --remove-orphans

# 4) Pospravi neuporabljene image-e (sprosti disk)
echo "$LOG_PREFIX docker image prune..."
docker image prune -f

echo "$LOG_PREFIX Deploy uspešno končan."

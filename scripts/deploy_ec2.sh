#!/usr/bin/env bash
# deploy_ec2.sh — Build images locally, push to Docker Hub, pull & restart on EC2.
# Also extracts frontend static files and deploys nginx site config.
#
# Usage:
#   ./scripts/deploy_ec2.sh <path-to-pem> <user@host>
#
# Prerequisites:
#   - docker login (local machine)
#   - docker login (EC2 instance)
#   - .env.prod exists on EC2 at ~/tasktracker/.env.prod
#   - VITE_GOOGLE_CLIENT_ID env var is set

set -euo pipefail

PEM_KEY="${1:?Usage: deploy_ec2.sh <path-to-pem> <user@host>}"
EC2_HOST="${2:?Usage: deploy_ec2.sh <path-to-pem> <user@host>}"
DOCKER_USER="grabs46"
REMOTE_DIR="~/tasktracker"

echo "==> Building backend image..."
docker build -t "${DOCKER_USER}/tasktracker-api:latest" -f backend/Dockerfile ./backend

echo "==> Building frontend image..."
docker build \
  --build-arg VITE_API_BASE=https://tasktracker.nicolasgrabner.com \
  --build-arg VITE_GOOGLE_CLIENT_ID="${VITE_GOOGLE_CLIENT_ID:?Set VITE_GOOGLE_CLIENT_ID env var}" \
  -t "${DOCKER_USER}/tasktracker-nginx:latest" \
  -f nginx/Dockerfile .

echo "==> Extracting frontend static files from image..."
CONTAINER_ID=$(docker create "${DOCKER_USER}/tasktracker-nginx:latest")
rm -rf ./frontend-dist
mkdir -p ./frontend-dist
docker cp "${CONTAINER_ID}:/usr/share/nginx/html/." ./frontend-dist
docker rm "${CONTAINER_ID}"

echo "==> Pushing backend image to Docker Hub..."
docker push "${DOCKER_USER}/tasktracker-api:latest"

echo "==> Copying files to EC2..."
scp -i "${PEM_KEY}" docker-compose.prod.yml "${EC2_HOST}:${REMOTE_DIR}/docker-compose.prod.yml"
scp -i "${PEM_KEY}" nginx/tasktracker.conf "${EC2_HOST}:${REMOTE_DIR}/tasktracker.conf"
scp -i "${PEM_KEY}" -r ./frontend-dist/* "${EC2_HOST}:${REMOTE_DIR}/frontend-dist/"

echo "==> Deploying on EC2..."
ssh -i "${PEM_KEY}" "${EC2_HOST}" bash -s <<'REMOTE'
  cd ~/tasktracker

  # Deploy frontend static files
  sudo rm -rf /var/www/tasktracker/*
  sudo mkdir -p /var/www/tasktracker
  sudo cp -r frontend-dist/* /var/www/tasktracker/
  sudo chmod -R 755 /var/www/tasktracker
  rm -rf frontend-dist

  # Deploy nginx site config
  sudo cp tasktracker.conf /etc/nginx/conf.d/tasktracker.conf
  sudo nginx -t && sudo systemctl reload nginx

  # Pull and restart backend container
  docker compose -f docker-compose.prod.yml pull
  docker compose -f docker-compose.prod.yml up -d
  docker image prune -f
REMOTE

# Clean up local extraction
rm -rf ./frontend-dist

echo "==> Deploy complete. Smoke test: ./scripts/smoke.sh"

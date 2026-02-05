#!/usr/bin/env bash
# setup_ssl.sh — Run on EC2 to provision Let's Encrypt certs and switch Nginx to HTTPS.
#
# Prerequisites:
#   - certbot installed (sudo yum install -y certbot)
#   - Containers running with nginx.initial.conf (HTTP-only)
#   - DNS A record for nicolasgrabner.com -> EC2 IP
#
# Usage:
#   ssh -i key.pem ec2-user@3.147.133.1
#   ./setup_ssl.sh your@email.com

set -euo pipefail

EMAIL="${1:?Usage: setup_ssl.sh <your-email>}"
DOMAIN="tasktracker.nicolasgrabner.com"
COMPOSE_FILE="docker-compose.prod.yml"
WEBROOT="/var/www/certbot"

echo "==> Creating certbot webroot directory..."
sudo mkdir -p "${WEBROOT}"

echo "==> Requesting certificate from Let's Encrypt..."
sudo certbot certonly \
  --webroot \
  -w "${WEBROOT}" \
  -d "${DOMAIN}" \
  --email "${EMAIL}" \
  --agree-tos \
  --non-interactive

echo "==> Certificate obtained. Restarting containers to pick up certs..."
cd ~/tasktracker
docker compose -f "${COMPOSE_FILE}" down
docker compose -f "${COMPOSE_FILE}" up -d

echo "==> Setting up auto-renewal cron job..."
(sudo crontab -l 2>/dev/null; echo "0 */12 * * * certbot renew --quiet --webroot -w ${WEBROOT} && docker exec tasktracker-nginx nginx -s reload") | sudo crontab -

echo "==> SSL setup complete!"
echo "    Test: curl -I https://${DOMAIN}"
echo "    Certs auto-renew via cron every 12 hours."

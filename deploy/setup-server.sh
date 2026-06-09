#!/usr/bin/env bash
# Initial server setup. Run as root on a fresh Ubuntu 22.04/24.04 VPS.
# Usage: sudo bash setup-server.sh YOUR_DOMAIN
set -euo pipefail

DOMAIN="${1:-}"
if [[ -z "$DOMAIN" ]]; then
    echo "Usage: sudo bash setup-server.sh YOUR_DOMAIN"
    exit 1
fi

echo "==> Installing Java 21, nginx, certbot..."
apt-get update -qq
apt-get install -y openjdk-21-jre-headless nginx certbot python3-certbot-nginx

echo "==> Creating astrax user and directories..."
id -u astrax &>/dev/null || useradd --system --home /opt/astrax --shell /usr/sbin/nologin astrax
mkdir -p /opt/astrax/data /opt/astrax/backend
chown -R astrax:astrax /opt/astrax

echo "==> Configuring nginx..."
sed "s/YOUR_DOMAIN/$DOMAIN/g" nginx-astrax.conf > /etc/nginx/sites-available/astrax
ln -sf /etc/nginx/sites-available/astrax /etc/nginx/sites-enabled/astrax
rm -f /etc/nginx/sites-enabled/default
nginx -t && systemctl reload nginx

echo "==> Obtaining SSL certificate..."
certbot --nginx -d "$DOMAIN" --non-interactive --agree-tos --register-unsafely-without-email

echo "==> Installing systemd service..."
cp astrax-backend.service /etc/systemd/system/
systemctl daemon-reload
systemctl enable astrax-backend

echo ""
echo "Setup complete. Next steps:"
echo "  1. Copy deploy/env.example to /opt/astrax/.env and set ASTRAX_JWT_SECRET"
echo "  2. Set ASTRAX_CORS_HOSTS=$DOMAIN in /opt/astrax/.env"
echo "  3. Run deploy.sh from your local machine to upload the backend"
echo "  4. Set astrax.baseUrl=https://$DOMAIN in local.properties and rebuild Android"

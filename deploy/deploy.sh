#!/usr/bin/env bash
# Build and deploy backend to a remote VPS.
# Usage: ./deploy.sh user@host
set -euo pipefail

REMOTE="${1:-}"
if [[ -z "$REMOTE" ]]; then
    echo "Usage: ./deploy.sh user@host"
    echo "Example: ./deploy.sh root@123.45.67.89"
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "==> Building backend..."
cd "$PROJECT_DIR"
./gradlew :backend:installDist -q

echo "==> Uploading to $REMOTE..."
rsync -avz --delete \
    --exclude '._*' \
    "$PROJECT_DIR/backend/build/install/backend/" \
    "$REMOTE:/opt/astrax/backend/"

echo "==> Restarting service..."
ssh "$REMOTE" "chown -R astrax:astrax /opt/astrax/backend && systemctl restart astrax-backend && systemctl status astrax-backend --no-pager"

echo ""
echo "Deploy complete. Check health:"
echo "  curl https://YOUR_DOMAIN/health"

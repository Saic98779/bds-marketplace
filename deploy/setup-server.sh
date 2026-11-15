#!/usr/bin/env bash
# One-time (idempotent) server setup for bds-backend on Ubuntu.
#   - Docker + Oracle Database 23ai Free (gvenzl/oracle-free) on 127.0.0.1:1521
#   - Java 21 + Maven for Jenkins builds (installed side by side; the system default java is left unchanged)
#   - /opt/bds layout, systemd service, generated secrets in /etc/bds/bds-backend.env
#   - sudo rule letting the jenkins user run only the deploy script
#
# Usage: sudo bash setup-server.sh [backend_port]
set -euo pipefail

PORT="${1:-8090}"
APP_USER=bds
APP_HOME=/opt/bds
ENV_FILE=/etc/bds/bds-backend.env
ORACLE_CONTAINER=bds-oracle
HERE="$(cd "$(dirname "$0")" && pwd)"

[ "$(id -u)" -eq 0 ] || { echo "Run with sudo"; exit 1; }

echo "==> Packages"
apt-get update -qq
# Other services on this host use the default java; keep whatever it points at today.
SELECTIONS=$(update-alternatives --get-selections)
apt-get install -y -qq ca-certificates curl openssl openjdk-21-jdk-headless maven >/dev/null
echo "$SELECTIONS" | update-alternatives --set-selections
echo "    default java: $(readlink -f /usr/bin/java)"
if ! command -v docker >/dev/null; then
    curl -fsSL https://get.docker.com | sh
fi
systemctl enable --now docker

echo "==> Secrets"
mkdir -p /etc/bds
if [ ! -f "$ENV_FILE" ]; then
    gen() { openssl rand -base64 48 | tr -d '\n/+=' | cut -c1-48; }
    DB_PASSWORD="Bds$(openssl rand -hex 8)#1"
    ADMIN_PASSWORD="Adm$(openssl rand -hex 6)@1"
    cat > "$ENV_FILE" <<EOF
PORT=$PORT
DB_URL=jdbc:oracle:thin:@//127.0.0.1:1521/FREEPDB1
DB_USERNAME=bds
DB_PASSWORD=$DB_PASSWORD
ORACLE_SYS_PASSWORD=Sys$(openssl rand -hex 8)#1
BDS_JWT_SECRET=$(gen)
BDS_DATA_KEY=$(gen)
BDS_HASH_KEY=$(gen)
BDS_ADMIN_PASSWORD=$ADMIN_PASSWORD
BDS_STORAGE_ROOT=$APP_HOME/data
BDS_EXPOSE_OTP=true
BDS_ALLOWED_ORIGINS=http://localhost:5173
EOF
    chmod 600 "$ENV_FILE"
    echo "    generated $ENV_FILE (admin password: $ADMIN_PASSWORD)"
fi
# shellcheck disable=SC1090
set -a; . "$ENV_FILE"; set +a

echo "==> Oracle Database Free"
if ! docker ps -a --format '{{.Names}}' | grep -qx "$ORACLE_CONTAINER"; then
    docker run -d --name "$ORACLE_CONTAINER" --restart unless-stopped \
        -p 127.0.0.1:1521:1521 \
        -e ORACLE_PASSWORD="$ORACLE_SYS_PASSWORD" \
        -e APP_USER="$DB_USERNAME" -e APP_USER_PASSWORD="$DB_PASSWORD" \
        -v bds-oracle-data:/opt/oracle/oradata \
        gvenzl/oracle-free:slim-faststart
fi
echo -n "    waiting for database"
for _ in $(seq 1 90); do
    if docker logs "$ORACLE_CONTAINER" 2>&1 | grep -q "DATABASE IS READY TO USE"; then echo " ready"; break; fi
    echo -n "."; sleep 5
done

echo "==> Application layout"
id "$APP_USER" >/dev/null 2>&1 || useradd --system --home "$APP_HOME" --shell /usr/sbin/nologin "$APP_USER"
mkdir -p "$APP_HOME/data" "$APP_HOME/releases" "$APP_HOME/bin"
chown -R "$APP_USER:$APP_USER" "$APP_HOME/data"
install -m 755 -o root -g root "$HERE/deploy.sh" "$APP_HOME/bin/deploy.sh"
install -m 644 -o root -g root "$HERE/bds-backend.service" /etc/systemd/system/bds-backend.service
systemctl daemon-reload
systemctl enable bds-backend >/dev/null

echo "==> Jenkins permissions"
if id jenkins >/dev/null 2>&1; then
    echo "jenkins ALL=(root) NOPASSWD: $APP_HOME/bin/deploy.sh" > /etc/sudoers.d/bds-deploy
    chmod 440 /etc/sudoers.d/bds-deploy
    visudo -cf /etc/sudoers.d/bds-deploy >/dev/null
else
    echo "    no local 'jenkins' user found; add a sudo rule for the Jenkins agent user manually"
fi

echo "Done. Deploy with: sudo $APP_HOME/bin/deploy.sh <path-to-bds-backend.jar>"

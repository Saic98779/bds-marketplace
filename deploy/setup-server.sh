#!/usr/bin/env bash
# One-time (idempotent) server setup for the bds-backend container on Ubuntu.
#   - Docker
#   - Oracle Database 23ai Free (gvenzl/oracle-free) on the private 'bds-net' network (host access: 127.0.0.1:1521)
#   - generated secrets in /etc/bds/bds-backend.env and the container env file for Jenkins in /etc/bds/bds-backend.container.env
#
# Usage: sudo bash setup-server.sh
set -euo pipefail

ENV_FILE=/etc/bds/bds-backend.env
CONTAINER_ENV_FILE=/etc/bds/bds-backend.container.env
ORACLE_CONTAINER=bds-oracle
NETWORK=bds-net

[ "$(id -u)" -eq 0 ] || { echo "Run with sudo"; exit 1; }

echo "==> Docker"
if ! command -v docker >/dev/null; then
    curl -fsSL https://get.docker.com | sh
fi
systemctl enable --now docker
docker network inspect "$NETWORK" >/dev/null 2>&1 || docker network create "$NETWORK" >/dev/null

echo "==> Secrets"
mkdir -p /etc/bds
if [ ! -f "$ENV_FILE" ]; then
    gen() { openssl rand -base64 48 | tr -d '\n/+=' | cut -c1-48; }
    ADMIN_PASSWORD="Adm$(openssl rand -hex 6)@1"
    cat > "$ENV_FILE" <<EOF
DB_USERNAME=bds
DB_PASSWORD=Bds$(openssl rand -hex 8)#1
ORACLE_SYS_PASSWORD=Sys$(openssl rand -hex 8)#1
BDS_JWT_SECRET=$(gen)
BDS_DATA_KEY=$(gen)
BDS_HASH_KEY=$(gen)
BDS_ADMIN_PASSWORD=$ADMIN_PASSWORD
BDS_EXPOSE_OTP=true
BDS_ALLOWED_ORIGINS=http://localhost:5173
EOF
    chmod 600 "$ENV_FILE"
    echo "    generated $ENV_FILE (admin password: $ADMIN_PASSWORD)"
fi
# shellcheck disable=SC1090
set -a; . "$ENV_FILE"; set +a

# What the application container needs: everything except the Oracle SYS password, plus the in-network DB URL.
{
    echo "DB_URL=jdbc:oracle:thin:@//$ORACLE_CONTAINER:1521/FREEPDB1"
    grep -vE '^(ORACLE_SYS_PASSWORD|DB_URL|PORT|BDS_STORAGE_ROOT)=' "$ENV_FILE"
} > "$CONTAINER_ENV_FILE"
chmod 600 "$CONTAINER_ENV_FILE"
echo "    container env file: $CONTAINER_ENV_FILE (upload it as Jenkins secret file 'bds-env-file')"

echo "==> Oracle Database Free"
if ! docker ps -a --format '{{.Names}}' | grep -qx "$ORACLE_CONTAINER"; then
    docker run -d --name "$ORACLE_CONTAINER" --restart unless-stopped \
        --network "$NETWORK" \
        -p 127.0.0.1:1521:1521 \
        -e ORACLE_PASSWORD="$ORACLE_SYS_PASSWORD" \
        -e APP_USER="$DB_USERNAME" -e APP_USER_PASSWORD="$DB_PASSWORD" \
        -v bds-oracle-data:/opt/oracle/oradata \
        gvenzl/oracle-free:slim-faststart
fi
docker network connect "$NETWORK" "$ORACLE_CONTAINER" 2>/dev/null || true
echo -n "    waiting for database"
for _ in $(seq 1 90); do
    if docker logs "$ORACLE_CONTAINER" 2>&1 | grep -q "DATABASE IS READY TO USE"; then echo " ready"; break; fi
    echo -n "."; sleep 5
done

echo "Done. Jenkins job 'bds-backend' builds the Dockerfile and runs the container on 127.0.0.1:8090."

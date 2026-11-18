#!/usr/bin/env bash
# One-time (idempotent) server setup for the bds-backend container on Ubuntu.
#   - Docker
#   - Oracle Database 23ai Free (gvenzl/oracle-free) on the private 'bds-net' network
#     (host access: 127.0.0.1:$ORACLE_HOST_PORT, default 1522 because other Oracle containers often hold 1521)
#   - generated secrets in /etc/bds/bds-backend.env and the container env file for Jenkins in /etc/bds/bds-backend.container.env
#
# Usage: sudo bash setup-server.sh
#        sudo EXISTING_ORACLE=oracle21c bash setup-server.sh   # reuse a running Oracle container instead:
#             creates the 'bds' user in its PDB (ORACLE_SERVICE, default XEPDB1) and joins it to 'bds-net'
set -euo pipefail

ENV_FILE=/etc/bds/bds-backend.env
CONTAINER_ENV_FILE=/etc/bds/bds-backend.container.env
NETWORK=bds-net
ORACLE_HOST_PORT=${ORACLE_HOST_PORT:-1522}
if [ -n "${EXISTING_ORACLE:-}" ]; then
    ORACLE_CONTAINER=$EXISTING_ORACLE
    ORACLE_SERVICE=${ORACLE_SERVICE:-XEPDB1}
else
    ORACLE_CONTAINER=bds-oracle
    ORACLE_SERVICE=FREEPDB1
fi

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
    echo "DB_URL=jdbc:oracle:thin:@//$ORACLE_CONTAINER:1521/$ORACLE_SERVICE"
    grep -vE '^(ORACLE_SYS_PASSWORD|DB_URL|PORT|BDS_STORAGE_ROOT)=' "$ENV_FILE"
} > "$CONTAINER_ENV_FILE"
chmod 600 "$CONTAINER_ENV_FILE"
echo "    container env file: $CONTAINER_ENV_FILE (upload it as Jenkins secret file 'bds-env-file')"

if [ -n "${EXISTING_ORACLE:-}" ]; then
    echo "==> Existing Oracle container '$ORACLE_CONTAINER' (PDB $ORACLE_SERVICE)"
    docker ps --format '{{.Names}}' | grep -qx "$ORACLE_CONTAINER" || { echo "    container '$ORACLE_CONTAINER' is not running"; exit 1; }
    docker network connect "$NETWORK" "$ORACLE_CONTAINER" 2>/dev/null || true
    # OS authentication inside the container, so the SYS password isn't needed. Creates the user only once.
    docker exec -i "$ORACLE_CONTAINER" sqlplus -s / as sysdba <<SQL
WHENEVER SQLERROR EXIT FAILURE
ALTER SESSION SET CONTAINER = $ORACLE_SERVICE;
DECLARE
    n NUMBER;
BEGIN
    SELECT COUNT(*) INTO n FROM dba_users WHERE username = UPPER('$DB_USERNAME');
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE USER $DB_USERNAME IDENTIFIED BY "$DB_PASSWORD" QUOTA UNLIMITED ON USERS';
        EXECUTE IMMEDIATE 'GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE VIEW, CREATE PROCEDURE, CREATE TRIGGER TO $DB_USERNAME';
        DBMS_OUTPUT.PUT_LINE('created');
    END IF;
END;
/
EXIT
SQL
    echo "    user '$DB_USERNAME' ready in $ORACLE_SERVICE"
else
    echo "==> Oracle Database Free"
    if ! docker ps -a --format '{{.Names}}' | grep -qx "$ORACLE_CONTAINER"; then
        if ss -ltn "sport = :$ORACLE_HOST_PORT" | grep -q LISTEN; then
            echo "    port $ORACLE_HOST_PORT is in use; re-run with ORACLE_HOST_PORT=<free port>"; exit 1
        fi
        docker run -d --name "$ORACLE_CONTAINER" --restart unless-stopped \
            --network "$NETWORK" \
            -p 127.0.0.1:$ORACLE_HOST_PORT:1521 \
            -e ORACLE_PASSWORD="$ORACLE_SYS_PASSWORD" \
            -e APP_USER="$DB_USERNAME" -e APP_USER_PASSWORD="$DB_PASSWORD" \
            -v bds-oracle-data:/opt/oracle/oradata \
            gvenzl/oracle-free:slim-faststart
    fi
    docker start "$ORACLE_CONTAINER" >/dev/null
    docker network connect "$NETWORK" "$ORACLE_CONTAINER" 2>/dev/null || true
    echo -n "    waiting for database"
    for _ in $(seq 1 90); do
        if docker logs "$ORACLE_CONTAINER" 2>&1 | grep -q "DATABASE IS READY TO USE"; then echo " ready"; break; fi
        echo -n "."; sleep 5
    done
fi

echo "Done. Jenkins job 'bds-backend' builds the Dockerfile and runs the container on 127.0.0.1:8090."

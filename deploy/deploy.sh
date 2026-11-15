#!/usr/bin/env bash
# Installs a bds-backend jar as the current release, restarts the service and waits for it to report healthy.
# Rolls back to the previous release if the new one does not come up.
#
# Usage: sudo /opt/bds/bin/deploy.sh /path/to/bds-backend.jar
set -euo pipefail

APP_HOME=/opt/bds
JAR="${1:?usage: deploy.sh <jar>}"
[ -f "$JAR" ] || { echo "No such jar: $JAR"; exit 1; }

PORT=$(grep -E '^PORT=' /etc/bds/bds-backend.env | cut -d= -f2)
RELEASE="$APP_HOME/releases/bds-backend-$(date +%Y%m%d%H%M%S).jar"
PREVIOUS=$(readlink -f "$APP_HOME/bds-backend.jar" 2>/dev/null || true)

install -m 644 -o root -g root "$JAR" "$RELEASE"
ln -sfn "$RELEASE" "$APP_HOME/bds-backend.jar"
systemctl restart bds-backend

healthy() {
    for _ in $(seq 1 60); do
        if curl -fs "http://127.0.0.1:$PORT/actuator/health" | grep -q '"UP"'; then return 0; fi
        sleep 3
    done
    return 1
}

if healthy; then
    echo "bds-backend is UP on port $PORT ($RELEASE)"
    ls -1t "$APP_HOME"/releases/*.jar | tail -n +6 | xargs -r rm -f
    exit 0
fi

echo "bds-backend failed its health check; recent log:"
journalctl -u bds-backend -n 60 --no-pager || true
if [ -n "$PREVIOUS" ] && [ -f "$PREVIOUS" ] && [ "$PREVIOUS" != "$RELEASE" ]; then
    echo "Rolling back to $PREVIOUS"
    ln -sfn "$PREVIOUS" "$APP_HOME/bds-backend.jar"
    systemctl restart bds-backend
    healthy && echo "Rollback is UP"
fi
exit 1

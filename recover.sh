#!/bin/bash
set -e

PEER_IP="${PEER_IP:-172.20.0.30}"
REPL_USER="repl_user"
REPL_PASS="repl_password"
POSTGRES_PASS="1234"
DATA_DIR="/bitnami/postgresql/data"
CONF_DIR="/opt/bitnami/postgresql/conf"

echo "[Recuperación] Verificando si el nodo par ($PEER_IP) está activo y es el Master..."

is_peer_master=false
# Check TCP reachability using bash dev tcp
if timeout 3 bash -c "exec 3<>/dev/tcp/$PEER_IP/5432" 2>/dev/null; then
    echo "[Recuperación] El nodo par ($PEER_IP) es alcanzable. Consultando el estado de recuperación..."
    recovery_status=$(PGPASSWORD="$POSTGRES_PASS" psql -h "$PEER_IP" -U postgres -d postgres -tA -c "SELECT pg_is_in_recovery();" 2>/dev/null || echo "error")
    echo "[Recuperación] El estado de recuperación del nodo par es: $recovery_status"
    if [ "$recovery_status" = "f" ]; then
        is_peer_master=true
    fi
else
    echo "[Recuperación] El nodo par ($PEER_IP) no es alcanzable en el puerto 5432."
fi

if [ "$is_peer_master" = "true" ]; then
    echo "[Recuperación] El Nodo 2 (peer) es el Master activo. Clonando sus datos nuevos antes de tomar el control..."
    
    echo "[Recuperación] Limpiando el directorio de datos antiguo..."
    rm -rf "$DATA_DIR"/*
    
    echo "[Recuperación] Clonando datos desde el Nodo 2 usando pg_basebackup..."
    PGPASSWORD="$REPL_PASS" pg_basebackup -h "$PEER_IP" -U "$REPL_USER" -D "$DATA_DIR" -Fp -Xs -P -R
    
    chmod 700 "$DATA_DIR"
    # IMPORTANT: Delete standby.signal so Node 1 starts as MASTER
    rm -f "$DATA_DIR/standby.signal"
    echo "[Recuperación] pg_basebackup completado. El Nodo 1 iniciará como MASTER."
fi

# Node 1 always starts/runs as Master
export POSTGRESQL_REPLICATION_MODE="master"

# Background loop to ensure replication entries and reload configuration
(
    while true; do
        if [ -f "$CONF_DIR/pg_hba.conf" ]; then
            if ! grep -q "host replication $REPL_USER" "$CONF_DIR/pg_hba.conf"; then
                echo "[Recuperación] Agregando/preservando la entrada de replicación en pg_hba.conf..."
                echo "host replication $REPL_USER 0.0.0.0/0 md5" >> "$CONF_DIR/pg_hba.conf"
            fi
            # Always attempt reload config (ignores errors if Postgres is not fully up yet)
            PGPASSWORD="$POSTGRES_PASS" pg_ctl reload -D "$DATA_DIR" >/dev/null 2>&1 || true
        fi
        sleep 5
    done
) &

# Finally, handover control to the default Bitnami PostgreSQL entrypoint
echo "[Recuperación] Iniciando PostgreSQL..."
exec /opt/bitnami/scripts/postgresql/entrypoint.sh /opt/bitnami/scripts/postgresql/run.sh

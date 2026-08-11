#!/usr/bin/env bash
# SACCOLink GUI - run script
#
# By default the Connect dialog is shown first so the user feeds in the
# Oracle connection details (host / port / service / user / password).
# To skip the dialog, pass --url / --user / --pass explicitly (or export
# the DB_HOST, DB_PORT, DB_SERVICE, DB_USER, DB_PASS variables).
set -euo pipefail
cd "$(dirname "$0")"

JARS=$(find lib -name '*.jar' 2>/dev/null || true)
CP="out"
if [ -n "$JARS" ]; then
    CP="$CP:$(echo "$JARS" | tr '\n' ':')"
fi

EXTRA=()
if [ -n "${DB_HOST:-}" ] || [ -n "${DB_PORT:-}" ] || [ -n "${DB_SERVICE:-}" ]; then
    JDBC_URL="jdbc:oracle:thin:@//${DB_HOST:-localhost}:${DB_PORT:-1521}/${DB_SERVICE:-XEPDB1}"
    EXTRA+=(--url "$JDBC_URL")
    if [ -n "${DB_USER:-}" ]; then
        EXTRA+=(--user "$DB_USER")
    fi
    if [ -n "${DB_PASS:-}" ]; then
        EXTRA+=(--pass "$DB_PASS")
    fi
elif [ -n "${DB_USER:-}" ] || [ -n "${DB_PASS:-}" ]; then
    echo "Warning: DB_USER/DB_PASS ignored - set at least one of" >&2
    echo "         DB_HOST, DB_PORT or DB_SERVICE to skip the Connect dialog." >&2
fi

exec java -cp "$CP" saccolink.Main "${EXTRA[@]}" "$@"

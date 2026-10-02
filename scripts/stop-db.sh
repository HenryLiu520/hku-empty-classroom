#!/usr/bin/env bash
# 停止本项目专用 PostgreSQL（只影响 db/pgdata 这个实例）
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PGBIN="/opt/homebrew/opt/postgresql@17/bin"
DATA="$ROOT/db/pgdata"

export PATH="$PGBIN:$PATH"
export LC_ALL="${LC_ALL:-C}"
export LANG="${LANG:-C}"

if pg_ctl -D "$DATA" status >/dev/null 2>&1; then
  pg_ctl -D "$DATA" -m fast stop
  echo "[db] stopped"
else
  echo "[db] not running"
fi

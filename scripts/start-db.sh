#!/usr/bin/env bash
# 启动本项目专用的 PostgreSQL（数据目录在项目内 db/pgdata，端口 5433，不影响系统其它服务）
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PGBIN="/opt/homebrew/opt/postgresql@17/bin"
DATA="$ROOT/db/pgdata"
LOG="$ROOT/db/pg.log"
PORT=5433
DB=emptyclassroom

export PATH="$PGBIN:$PATH"
# macOS 下 PostgreSQL 17 必须有有效 locale，否则报 "postmaster became multithreaded during startup"
export LC_ALL="${LC_ALL:-C}"
export LANG="${LANG:-C}"

if [ ! -d "$DATA" ]; then
  echo "[db] first run: initdb into $DATA"
  mkdir -p "$DATA"
  initdb -D "$DATA" -U "$USER" --auth=trust --encoding=UTF8 --locale=C >/dev/null
fi

if pg_ctl -D "$DATA" status >/dev/null 2>&1; then
  echo "[db] already running on port $PORT"
else
  echo "[db] starting on port $PORT"
  pg_ctl -D "$DATA" -o "-p $PORT -k /tmp" -l "$LOG" start
  sleep 2
fi

if ! psql -h localhost -p $PORT -U "$USER" -lqt | cut -d '|' -f1 | grep -qw "$DB"; then
  echo "[db] creating database $DB"
  createdb -h localhost -p $PORT -U "$USER" "$DB"
fi

echo "[db] ready: postgresql://$USER@localhost:$PORT/$DB"
echo "[db] log: $LOG   stop: scripts/stop-db.sh"

#!/usr/bin/env bash
# 停止后端、前端与数据库
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

for port in 8080 5173; do
  pids=$(lsof -ti :$port -sTCP:LISTEN 2>/dev/null || true)
  if [ -n "$pids" ]; then
    echo "[stop] killing listener on $port: $pids"
    kill $pids 2>/dev/null || true
  fi
done

# spring-boot:run 会派生子进程，必要时按端口再兜一次
sleep 1
for port in 8080 5173; do
  pids=$(lsof -ti :$port -sTCP:LISTEN 2>/dev/null || true)
  [ -n "$pids" ] && kill -9 $pids 2>/dev/null || true
done

"$ROOT/scripts/stop-db.sh"
echo "[stop] done"

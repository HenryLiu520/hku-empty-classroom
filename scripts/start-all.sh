#!/usr/bin/env bash
# 一键启动：数据库 → 后端 → 前端（后端与前端日志写到 logs/）
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
mkdir -p "$ROOT/logs"

"$ROOT/scripts/start-db.sh"

if lsof -i :8080 -sTCP:LISTEN >/dev/null 2>&1; then
  echo "[backend] already listening on 8080"
else
  echo "[backend] starting … (log: logs/backend.log)"
  nohup "$ROOT/scripts/start-backend.sh" > "$ROOT/logs/backend.log" 2>&1 &
fi

echo "[backend] waiting for http://localhost:8080/api/buildings …"
for i in $(seq 1 90); do
  if curl -sf http://localhost:8080/api/buildings >/dev/null 2>&1; then
    echo "[backend] up"
    break
  fi
  sleep 2
done

if lsof -i :5173 -sTCP:LISTEN >/dev/null 2>&1; then
  echo "[frontend] already listening on 5173"
else
  echo "[frontend] starting … (log: logs/frontend.log)"
  nohup "$ROOT/scripts/start-frontend.sh" > "$ROOT/logs/frontend.log" 2>&1 &
fi

echo
echo "  Student view : http://localhost:5173/login   (user1 / user123, read only)"
echo "  Admin port   : http://localhost:5173/manage  (admin1 / admin123, add or release room use time)"
echo "  API          : http://localhost:8080/api/availability?date=$(date +%F)\&building=Central%20Podium\&from=14:00\&to=15:50"
echo
echo "  Stop everything: scripts/stop-all.sh"

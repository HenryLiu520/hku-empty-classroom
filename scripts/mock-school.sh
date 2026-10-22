#!/usr/bin/env bash
# 模拟学校课表接口（test double）的启动/停止/状态脚本。
# 端口 9000，只绑定 127.0.0.1。就绪判断用真实信号（/v1/meta 返回 200），不猜秒数。
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DIR="$ROOT/mock-school-api"
LOG="$ROOT/logs/mock-school.log"
PIDFILE="$ROOT/logs/mock-school.pid"
PORT=9000
KEY="${MOCK_API_KEY:-hku-demo-key}"

probe() {
  curl -s -o /dev/null -w '%{http_code}' --max-time 2 -H "X-Api-Key: $KEY" "http://127.0.0.1:$PORT/v1/meta" || true
}

start() {
  if [ -f "$PIDFILE" ] && kill -0 "$(cat "$PIDFILE")" 2>/dev/null; then
    echo "[mock-school] already running (pid $(cat "$PIDFILE"))"
    return 0
  fi
  mkdir -p "$ROOT/logs"
  echo "[mock-school] building …"
  ( cd "$DIR" && mvn -q -DskipTests package )
  JAR="$(ls "$DIR"/target/mock-school-api-*.jar | head -1)"
  echo "[mock-school] starting $JAR"
  nohup java -jar "$JAR" >> "$LOG" 2>&1 &
  echo $! > "$PIDFILE"
  for _ in $(seq 1 90); do
    if [ "$(probe)" = "200" ]; then
      echo "[mock-school] up on http://127.0.0.1:$PORT (api key: $KEY)"
      return 0
    fi
    sleep 1
  done
  echo "[mock-school] did NOT come up; last log lines:" >&2
  tail -20 "$LOG" >&2
  return 1
}

stop() {
  if [ -f "$PIDFILE" ]; then
    PID="$(cat "$PIDFILE")"
    if kill -0 "$PID" 2>/dev/null; then
      kill "$PID"
      echo "[mock-school] stopped (pid $PID)"
    fi
    rm -f "$PIDFILE"
  else
    echo "[mock-school] not running"
  fi
}

status() {
  echo "[mock-school] HTTP $(probe) on port $PORT"
}

case "${1:-start}" in
  start) start ;;
  stop) stop ;;
  restart) stop; start ;;
  status) status ;;
  *) echo "usage: $0 {start|stop|restart|status}" >&2; exit 2 ;;
esac

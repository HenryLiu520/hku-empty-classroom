#!/usr/bin/env bash
# 等服务真的就绪：轮询真实信号（健康接口 + 端口），而不是猜一个秒数。
# 用法：scripts/wait-ready.sh [超时秒数，默认 120]
# 成功打印 "就绪"，失败打印后端日志末尾，退出码 1。
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LIMIT="${1:-120}"
START=$(date +%s)
backend=no
frontend=no

while :; do
  ready_b=no; ready_f=no
  curl -sf -o /dev/null http://localhost:8080/api/buildings && ready_b=yes
  curl -sf -o /dev/null http://localhost:5173/ && ready_f=yes
  backend="$ready_b"; frontend="$ready_f"
  ELAPSED=$(( $(date +%s) - START ))

  if [ "$backend" = yes ] && [ "$frontend" = yes ]; then
    printf '[%3ss] backend=yes frontend=yes  就绪\n' "$ELAPSED"
    exit 0
  fi
  if [ "$ELAPSED" -ge "$LIMIT" ]; then
    printf '[%3ss] backend=%s frontend=%s  超时 %ss\n' "$ELAPSED" "$backend" "$frontend" "$LIMIT"
    echo "--- logs/backend.log 末尾 ---"
    tail -15 "$ROOT/logs/backend.log"
    exit 1
  fi
  printf '[%3ss] backend=%s frontend=%s  …\n' "$ELAPSED" "$backend" "$frontend"
  sleep 1          # 轮询间隔，不是猜就绪时间
done

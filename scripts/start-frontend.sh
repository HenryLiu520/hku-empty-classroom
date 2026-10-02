#!/usr/bin/env bash
# 启动前端（Vite dev server, 端口 5173），首次会自动 npm install
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/frontend"

if [ ! -d node_modules ]; then
  echo "[frontend] installing dependencies (first run) …"
  npm install
fi

exec npm run dev

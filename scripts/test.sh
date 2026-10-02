#!/usr/bin/env bash
# 跑后端的自动化测试（六个边界算例 + 区间运算），需要数据库在跑
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
"$ROOT/scripts/start-db.sh" >/dev/null

cd "$ROOT/backend"
if [ -d "/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home" ]; then
  export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
fi

mvn -q test
echo
echo "=== 结果 ==="
grep -h "Tests run" target/surefire-reports/*.txt

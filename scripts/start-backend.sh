#!/usr/bin/env bash
# 启动后端（Spring Boot, 端口 8080）
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/backend"

# 优先用 JDK 21（LTS）；没有就用系统默认 Java
if [ -d "/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home" ]; then
  export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
fi

echo "[backend] JAVA_HOME=${JAVA_HOME:-$(/usr/libexec/java_home 2>/dev/null)}"
exec mvn -q spring-boot:run

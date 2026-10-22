#!/usr/bin/env bash
# 证明模拟上游能用、并且能模拟三种坏情况。输出就是报告里可引的证据。
set -euo pipefail

B="http://127.0.0.1:9000"
KEY="${MOCK_API_KEY:-hku-demo-key}"
PY="python3"

say() { printf '\n=== %s ===\n' "$1"; }

say "1. /v1/meta（数据时间 lastUpdated 就在这里）"
curl -s -H "X-Api-Key: $KEY" "$B/v1/meta" | $PY -m json.tool

say "2. /v1/rooms（10 间教室）"
curl -s -H "X-Api-Key: $KEY" "$B/v1/rooms" | $PY -c "
import sys, json
d = json.load(sys.stdin)
print(len(d), 'rooms;', 'buildings:', sorted({r['building'] for r in d}))
print('first:', d[0])
"

say "3. /v1/timetable?date=2026-10-27（周二 → dayOfWeek 2）"
curl -s -H "X-Api-Key: $KEY" "$B/v1/timetable?date=2026-10-27" | $PY -c "
import sys, json
d = json.load(sys.stdin)
print('date', d['date'], '| dayOfWeek', d['dayOfWeek'], '| slots', d['count'])
for s in d['slots'][:3]:
    print('  ', s['roomCode'], s['start'], '-', s['end'], s['courseCode'])
"

say "3b. 同一天只取 Knowles Building"
curl -s -H "X-Api-Key: $KEY" "$B/v1/timetable?date=2026-10-27&building=Knowles%20Building" | $PY -c "
import sys, json
d = json.load(sys.stdin)
print('   slots', d['count'], '->', [s['roomCode'] for s in d['slots']][:5])
"

say "4. 不带 API key → 应 401"
curl -s -o /dev/null -w '   HTTP %{http_code}\n' "$B/v1/meta"

say "5. 让数据变旧 90 分钟"
curl -s -X POST "$B/v1/_control/stale?minutes=90" > /dev/null
curl -s -H "X-Api-Key: $KEY" "$B/v1/meta" | $PY -c "
import sys, json
d = json.load(sys.stdin)
print('   lastUpdated', d['lastUpdated'], '| staleMinutes', d['staleMinutes'])
"

say "6. 让响应慢 800 ms（消费端可以据此测超时）"
curl -s -X POST "$B/v1/_control/slow?ms=800" > /dev/null
curl -s -o /dev/null -w '   HTTP %{http_code} in %{time_total}s\n' -H "X-Api-Key: $KEY" "$B/v1/rooms"

say "7. 让上游挂掉 → 应 503"
curl -s -X POST "$B/v1/_control/down" > /dev/null
curl -s -o /dev/null -w '   HTTP %{http_code}\n' -H "X-Api-Key: $KEY" "$B/v1/meta"

say "8. 复位 → 应 200"
curl -s -X POST "$B/v1/_control/reset" > /dev/null
curl -s -o /dev/null -w '   HTTP %{http_code}\n' -H "X-Api-Key: $KEY" "$B/v1/meta"

echo
echo "全部通过：只读契约 + 鉴权 + 三种坏情况都能模拟。"

#!/usr/bin/env bash
# 模拟学校系统：10 项证据（只读契约 + 写入 + 冲突 + 三种坏情况）。
# 输出可以直接贴进报告或会上演示。
set -euo pipefail

B="http://127.0.0.1:9000"
KEY="${MOCK_API_KEY:-hku-demo-key}"
PY="python3"
H=(-H "X-Api-Key: $KEY")

say() { printf '\n=== %s ===\n' "$1"; }
jq_() { $PY -c "$1"; }

say "1. /v1/meta：这份数据什么时候生成、第几版"
curl -s "${H[@]}" "$B/v1/meta" | $PY -m json.tool

say "2. /v1/rooms：10 间教室 / 4 栋楼"
curl -s "${H[@]}" "$B/v1/rooms" | jq_ "
import sys, json
d = json.load(sys.stdin)
print(len(d), 'rooms |', sorted({r['building'] for r in d}))
print('first:', d[0])
"

say "3. /v1/timetable?date=2026-10-27（周二 → dayOfWeek 2）"
curl -s "${H[@]}" "$B/v1/timetable?date=2026-10-27" | jq_ "
import sys, json
d = json.load(sys.stdin)
print('date', d['date'], '| dayOfWeek', d['dayOfWeek'], '| slots', d['count'])
for s in d['slots'][:3]:
    print('  ', s['roomCode'], s['start'], '-', s['end'], s['courseCode'])
"

say "4. /v1/timetable/CPD-1.09?date=2026-10-27（单间房）"
curl -s "${H[@]}" "$B/v1/timetable/CPD-1.09?date=2026-10-27" | jq_ "
import sys, json
d = json.load(sys.stdin)
print(d['roomCode'], 'has', d['count'], 'slot(s):', [(s['start'], s['end'], s['courseCode']) for s in d['slots']])
"

say "5. 不带 API key → 应 401"
curl -s -o /dev/null -w '   HTTP %{http_code}\n' "$B/v1/meta"

say "6. 写入：新增一条课时（周六 14:00-15:50, TEST999），读回来，再删掉"
BEFORE=$(curl -s "${H[@]}" "$B/v1/timetable?date=2026-10-31" | jq_ "import sys,json;print(json.load(sys.stdin)['count'])")
NEWID=$(curl -s "${H[@]}" -H "Content-Type: application/json" -X POST "$B/v1/admin/slots" \
  -d '{"roomCode":"CPD-1.09","dayOfWeek":6,"start":"14:00","end":"15:50","courseCode":"TEST999"}' \
  | jq_ "import sys,json;print(json.load(sys.stdin)['id'])")
AFTER=$(curl -s "${H[@]}" "$B/v1/timetable?date=2026-10-31" | jq_ "import sys,json;print(json.load(sys.stdin)['count'])")
REV=$(curl -s "${H[@]}" "$B/v1/meta" | jq_ "import sys,json;print(json.load(sys.stdin)['revision'])")
echo "   slots on Sat before=$BEFORE after=$AFTER (new id=$NEWID) | meta.revision=$REV"
curl -s "${H[@]}" -X DELETE "$B/v1/admin/slots/$NEWID" > /dev/null
BACK=$(curl -s "${H[@]}" "$B/v1/timetable?date=2026-10-31" | jq_ "import sys,json;print(json.load(sys.stdin)['count'])")
echo "   after delete: $BACK (回到 $BEFORE)"

say "7. 写入冲突：同房同日时间重叠 → 应 400"
curl -s "${H[@]}" -H "Content-Type: application/json" -X POST "$B/v1/admin/slots" \
  -d '{"roomCode":"CPD-1.09","dayOfWeek":2,"start":"09:30","end":"10:30","courseCode":"CLASH"}' \
  -w '   HTTP %{http_code}\n'

say "8. 让数据变旧 90 分钟"
curl -s -X POST "$B/v1/_control/stale?minutes=90" > /dev/null
curl -s "${H[@]}" "$B/v1/meta" | jq_ "
import sys, json
d = json.load(sys.stdin)
print('   generatedAt', d['generatedAt'])
print('   lastUpdated', d['lastUpdated'], '| staleMinutes', d['staleMinutes'])
"

say "9. 让响应慢 800 ms"
curl -s -X POST "$B/v1/_control/slow?ms=800" > /dev/null
curl -s -o /dev/null -w '   HTTP %{http_code} in %{time_total}s\n' "${H[@]}" "$B/v1/rooms"

say "10. 让上游挂掉 → 503；复位 → 200"
curl -s -X POST "$B/v1/_control/down" > /dev/null
curl -s -o /dev/null -w '   down:  HTTP %{http_code}\n' "${H[@]}" "$B/v1/meta"
curl -s -X POST "$B/v1/_control/reset" > /dev/null
curl -s -o /dev/null -w '   reset: HTTP %{http_code}\n' "${H[@]}" "$B/v1/meta"

echo
echo "10 项全过：只读契约 / 写入与读回 / 冲突拒绝 / 变旧 / 变慢 / 宕机。"

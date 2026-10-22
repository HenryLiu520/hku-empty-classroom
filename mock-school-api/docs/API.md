# API 参考 — mock-school-api

模拟的香港大学课表接口（**test double**，不是官方服务）。
Base URL：`http://127.0.0.1:9000`　·　API 版本：`v1`　·　默认 API key：`hku-demo-key`

所有 `/v1/**` 端点（`_control` 除外）都要求请求头：

```
X-Api-Key: hku-demo-key
```

---

## 0. 约定

| 项 | 说明 |
|---|---|
| 时间格式 | `HH:mm`（24 小时制，香港时区） |
| 日期格式 | `YYYY-MM-DD` |
| 星期 | `dayOfWeek`：周一 = 1 … 周日 = 7 |
| 课表语义 | **周课表**：给定日期 → 换算成星期几 → 查该天的课 |
| 数据时间 | `generatedAt` = 数据生成时刻；`lastUpdated` = 对外声称的数据时刻（受 `_control/stale` 影响） |
| 数据版本 | `revision` 每次写操作 +1，消费端可据此判断课表有没有变 |
| 未知值 | 教室的 `sockets` 为 `null` 表示**未知**，不是 `false` |
| 错误体 | `{"error": "BAD_REQUEST", "message": "..."}` |

---

## 1. 只读端点

### 1.1 `GET /v1/meta`

数据源元信息。**消费端判断「数据有多新」就看这里。**

```bash
curl -s -H "X-Api-Key: hku-demo-key" http://127.0.0.1:9000/v1/meta
```

```json
{
  "provider": "HKU timetable (simulated)",
  "term": "2026/27 Semester 1",
  "version": "v1",
  "note": "Test double: a simulated University timetable feed, not an official service.",
  "generatedAt": "2026-10-22T05:08:24.592001Z",
  "lastUpdated": "2026-10-22T05:08:24.592001Z",
  "revision": 1,
  "now": "2026-10-22T05:08:36.306755Z",
  "staleMinutes": 0,
  "rooms": 10,
  "classSlots": 79
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| `generatedAt` | string (ISO-8601) | 数据生成时刻（写操作会更新时间） |
| `lastUpdated` | string (ISO-8601) | 对外声称的数据时刻 = `generatedAt` − `staleMinutes` |
| `revision` | int | 数据版本，写操作 +1 |
| `staleMinutes` | int | 当前 `stale` 开关的值（真接口不会有这个字段） |
| `rooms` / `classSlots` | int | 记录条数 |

### 1.2 `GET /v1/rooms`

```bash
curl -s -H "X-Api-Key: hku-demo-key" "http://127.0.0.1:9000/v1/rooms?building=Knowles%20Building"
```

```json
[
  {"code": "KB-110", "building": "Knowles Building", "floor": "1F",
   "capacity": 80, "sockets": null, "seatType": "Fixed rows"}
]
```

| 查询参数 | 必填 | 说明 |
|---|---|---|
| `building` | 否 | 楼栋全称，大小写不敏感；省略则返回全部 |

### 1.3 `GET /v1/timetable?date=`

某一天的全部课时。

```bash
curl -s -H "X-Api-Key: hku-demo-key" "http://127.0.0.1:9000/v1/timetable?date=2026-10-27"
```

```json
{
  "date": "2026-10-27",
  "dayOfWeek": 2,
  "lastUpdated": "2026-10-22T05:08:24.592001Z",
  "count": 21,
  "slots": [
    {"id": 17, "roomCode": "CPD-3.12", "building": "Central Podium", "dayOfWeek": 2,
     "start": "09:00", "end": "10:50", "courseCode": "COMP1110"}
  ]
}
```

| 查询参数 | 必填 | 说明 |
|---|---|---|
| `date` | 是 | `YYYY-MM-DD` |
| `building` | 否 | 只要这栋楼的课 |

### 1.4 `GET /v1/timetable/{roomCode}?date=`

单间房某一天的课时。房号不存在 → `404 NOT_FOUND`。

```bash
curl -s -H "X-Api-Key: hku-demo-key" \
  "http://127.0.0.1:9000/v1/timetable/CPD-1.09?date=2026-10-27"
```

```json
{
  "roomCode": "CPD-1.09", "date": "2026-10-27", "dayOfWeek": 2,
  "lastUpdated": "2026-10-22T05:08:24.592001Z", "count": 3,
  "slots": [
    {"id": 1, "roomCode": "CPD-1.09", "building": "Central Podium", "dayOfWeek": 2,
     "start": "09:00", "end": "10:50", "courseCode": "COMP1110"},
    {"id": 5, "roomCode": "CPD-1.09", "building": "Central Podium", "dayOfWeek": 2,
     "start": "11:00", "end": "12:50", "courseCode": "COMP1117"},
    {"id": 9, "roomCode": "CPD-1.09", "building": "Central Podium", "dayOfWeek": 2,
     "start": "14:00", "end": "15:50", "courseCode": "MATH1013"}
  ]
}
```

---

## 2. 写端点（「教务处那一侧」）

真实接口我们只会读；这一组是模拟系统方便造场景用的。

### 2.1 `POST /v1/admin/rooms`

```bash
curl -s -H "X-Api-Key: hku-demo-key" -H "Content-Type: application/json" \
  -X POST http://127.0.0.1:9000/v1/admin/rooms \
  -d '{"code":"CPD-5.01","building":"Central Podium","floor":"5F","capacity":35,"sockets":true,"seatType":"Individual desks"}'
```

`201` 不返回，成功即 `200` + 新建对象。字段：`code`（唯一）、`building`、`floor`、`capacity` 必填。

### 2.2 `PATCH /v1/admin/rooms/{code}`

只改传了的字段（`null` 表示不改）。

```bash
curl -s -H "X-Api-Key: hku-demo-key" -H "Content-Type: application/json" \
  -X PATCH http://127.0.0.1:9000/v1/admin/rooms/CPD-5.01 \
  -d '{"capacity":40}'
```

### 2.3 `DELETE /v1/admin/rooms/{code}`

删除教室，**连带它的全部课时**（外键级联）。

### 2.4 `GET /v1/admin/rooms/{code}/slots`

某间房的完整周课表（不分日期）：

```json
[
  {"id": 1, "roomCode": "CPD-1.09", "building": "Central Podium", "dayOfWeek": 1,
   "start": "10:00", "end": "11:50", "courseCode": "CAES1001"}
]
```

### 2.5 `POST /v1/admin/slots`

```bash
curl -s -H "X-Api-Key: hku-demo-key" -H "Content-Type: application/json" \
  -X POST http://127.0.0.1:9000/v1/admin/slots \
  -d '{"roomCode":"CPD-1.09","dayOfWeek":6,"start":"14:00","end":"15:50","courseCode":"TEST999"}'
```

**规则**：`end` 必须晚于 `start`；同一间房同一天**不允许时间重叠**，冲突返回：

```json
{"error":"BAD_REQUEST","message":"overlaps an existing class: COMP1110 09:00-10:50"}
```

### 2.6 `DELETE /v1/admin/slots/{id}`

```json
{"deleted": 80}
```

---

## 3. 测试开关（`/v1/_control`，不需要 API key）

| 方法 | 端点 | 效果 |
|---|---|---|
| GET | `/v1/_control/state` | `{staleMinutes, slowMs, down}` |
| POST | `/v1/_control/stale?minutes=90` | 让 `lastUpdated` 比真实时间旧 90 分钟 |
| POST | `/v1/_control/slow?ms=800` | 每个 `/v1/**` 响应先睡 800 ms（上限 30000） |
| POST | `/v1/_control/down?value=true` | 只读与写端点直接 `503` |
| POST | `/v1/_control/reset` | 三个开关全部归零 |

```bash
curl -s -X POST "http://127.0.0.1:9000/v1/_control/stale?minutes=90"
curl -s -X POST "http://127.0.0.1:9000/v1/_control/reset"
```

---

## 4. 错误码

| HTTP | `error` | 触发条件 |
|---|---|---|
| 400 | `BAD_REQUEST` | 参数不合法：日期格式、`end ≤ start`、时间重叠、缺必填字段、房号已存在 |
| 401 | `UNAUTHORIZED` | 缺 `X-Api-Key` 或 key 不对 |
| 404 | `NOT_FOUND` | 房号 / 课时不存在 |
| 503 | `SERVICE_UNAVAILABLE` | `_control/down` 打开时（模拟上游宕机） |

---

## 5. 消费端应该怎么用它

1. 启动时读一次 `/v1/meta`，记住 `revision`。
2. 每次回答「哪间房空」之前，先看 `lastUpdated`：超过阈值就在回答里标注数据旧。
3. 定期再读 `/v1/meta`：`revision` 变了 → 课表变过 → 重新拉 `/v1/timetable`。
4. 任何非 200 响应都当作「上游不可用」，走降级（用最近一次成功的数据 + 明确标注）。

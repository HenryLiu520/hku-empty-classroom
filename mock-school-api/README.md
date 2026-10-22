# mock-school-api — 模拟的学校课表系统（simulated feed）

> ⚠️ **这是我们自己写的模拟系统，不是香港大学的官方接口。**
> 报告与演示里必须写成 *a simulated feed / stub service*，不能写成 "the University's API"。

一套**独立运行**的小系统：自己持有课表（PostgreSQL），对外提供只读 API（真接口将来会给的那部分），
外加一组控制开关，用来模拟「数据旧了 / 响应慢了 / 服务挂了」三种现实情况。

- 端口 **9000**（只绑 `127.0.0.1`）
- 自己的数据库 **`mockschool`**（与项目库 `emptyclassroom` 分开）
- 文档：[`docs/API.md`](docs/API.md)（人读）· [`docs/openapi.yaml`](docs/openapi.yaml)（OpenAPI 3.1，可导入 Postman / Swagger UI）
- 设计日志与架构图：[`design/`](design/)（英文 md / docx / pdf + 中英两张图）

---

## 为什么做这个

我们的系统要读学校课表（Plan §3.1：课表是**输入**，只读），但读取权限还没拿到（Plan §5.1 的 Plan B）。
而最难的恰恰是**这条边界**：

1. 上游数据是 2 小时前的，我们的回答还准吗？界面有没有说实话？
2. 上游要 3 秒才回，会不会把我们的请求拖死？
3. 上游直接挂了，我们是报错还是降级？

没有上游就没法回答这三问。所以这里做一个**能真正持有数据、能用 API 交互**的替身。

## 结构

```
            ┌──────────────────────────────┐
            │  mock-school-api  (this)     │
  curl ────▶│  :9000  Spring Boot 4        │
            │  ├─ /v1/**        只读契约    │──┐
            │  ├─ /v1/admin/**  写入        │  │
            │  └─ /v1/_control  测试开关    │  │
            └──────────────────────────────┘  │
                                              ▼
                                    PostgreSQL  (mockschool)
                                     ├─ rooms
                                     ├─ class_slots
                                     └─ feed_meta
```

```
src/main/java/hku/mock/
  MockSchoolApiApplication.java  启动
  ProviderState.java             三个开关（stale / slow / down）
  domain/   Room, ClassSlot, FeedMeta
  repo/     RoomRepository, ClassSlotRepository, FeedMetaRepository
  service/  TimetableService     读、写、冲突检查、revision 维护
  web/      PublicController     /v1/**（只读）
            AdminController      /v1/admin/**（写）
            ControlController    /v1/_control/**（测试开关）
            ApiKeyGuard          API key + 慢响应 + 宕机
            GlobalErrors         统一错误体 {error, message}
src/main/resources/db/migration/
  V1__schema.sql                  三张表
  V2__seed.sql                    种子数据（10 间房 / 79 条课时，从项目库导出）
```

## 数据模型

| 表 | 作用 | 关键字段 |
|---|---|---|
| `rooms` | 教室 | `code`（唯一）、`building`、`floor`、`capacity`、`sockets`（**null = 未知**）、`seat_type` |
| `class_slots` | 周课表 | `room_id`、`day_of_week`（周一 = 1）、`start_time`、`end_time`、`course_code` |
| `feed_meta` | 单行元信息 | `provider`、`term`、`generated_at`、`revision` |

**语义**：日期 → 星期几 → 查该天的课。`generated_at` 在每次写操作时更新，`revision` +1。

## 跑起来

```bash
# 首次：建库（脚本会做）
scripts/mock-school.sh start        # 首次会 mvn package，约 40 s
scripts/mock-school.sh status       # 打一次 /v1/meta 看状态

# 10 项证据：只读契约 / 写入与读回 / 冲突拒绝 / 变旧 / 变慢 / 宕机
scripts/verify-mock.sh

scripts/mock-school.sh stop
```

默认 API key：`hku-demo-key`（可用 `MOCK_API_KEY` 覆盖）。

## 控制台（可视化验证 + 随时改）

浏览器打开 **<http://127.0.0.1:9000/>** —— 页面由模拟服务自己托管（同源，无构建、无第二个前端工程）。

| 区块 | 能做什么 |
|---|---|
| **数据状态** | 看 `provider / term / generatedAt / lastUpdated / revision`，以及"数据是几分钟前的" |
| **测试开关** | 一键「变旧 60 分钟 / 变旧 3 小时 / 慢 800 ms / 慢 3 秒 / 挂掉 / 复位」；右上角徽章显示每个请求的状态码与耗时 |
| **课表网格** | 选日期（或点一～五）→ 教室 × 小时的矩阵，深色格是课（格内显示课号），浅绿是空闲 |
| **教室** | 直接改 building / floor / capacity / sockets / seatType，点保存就写进数据库；也能新增和删除（删除会连带课时） |
| **课时** | 选教室 → 看它的完整周课表，逐条删除；表单可新增课时，时间冲突会原样显示 400 错误 |

开关生效后刷新即可看到差异：例如点「变旧 3 小时」→ 数据状态里 `lastUpdated` 立刻退 3 小时 → 消费端就该据此提示数据旧。

## 验证结果（2026-10-22 实测）

```
meta            provider=HKU timetable (simulated), revision=1, rooms=10, classSlots=79
rooms           10 间 / 4 栋楼
timetable       2026-10-27（周二）→ 21 条课时
单间房          CPD-1.09 → 3 条（COMP1110 / COMP1117 / MATH1013）
无 API key      HTTP 401
写入后读回      周六课时数 0 → 1 → 0，revision 1 → 2
时间冲突        HTTP 400  {"error":"BAD_REQUEST","message":"overlaps an existing class: COMP1110 09:00-10:50"}
stale=90        lastUpdated 从 05:08Z 退到 03:38Z
slow=800        HTTP 200 in 0.810 s
down / reset    HTTP 503 / HTTP 200
```

## 设计决定（报告里可以写）

| 决定 | 理由 |
|---|---|
| **独立进程 + 独立数据库** | 只有跨进程才测得到「读外部服务」的真实边界：网络失败、超时、旧数据。放在自己库里测不到 |
| **`lastUpdated` 由数据时间决定** | Plan §3.1 承诺「回答要说明数据有多新」，这个字段就是兑现入口 |
| **`revision` 每次写 +1** | 消费端可以便宜地发现「课表变过」，不必每次全量比对 |
| **`sockets` 用 null 而不是 false** | 「不知道」和「没有」是两件事；写成 false 是撒谎 |
| **只读接口要 API key** | 真接口不会裸奔；这样「有权限 / 没权限」这条路径也被测到 |
| **控制开关不要 key** | 否则开关一开就关不掉 |
| **时间冲突在服务端拒绝** | 「课表」这一数据本身不允许同房同日重叠；这条规则属于上游，不属于我们的系统 |

## 没有做什么（limitations）

- **不写回真实系统**：模拟系统只在我们本机跑。
- **不做分页 / 增量同步**：数据量小（79 条），全量返回。
- **不做令牌签发 / 续期**：只校验一个固定 key，够测「有没有权限」这一层。
- **不模拟部分数据缺失**：可以加一个 `_control/partial` 开关，目前没做。
- **种子数据是一次性导出的**：真实学期开始后课表会变，这份数据不会自动更新。

## 数据来源

`V2__seed.sql` 由脚本从项目数据库导出（10 间教室 / 79 条课时 / 4 栋楼），**不是手写**。
要改数据：改项目库 → 重新导出 → 新增一个 Flyway 迁移（不要改已应用的 V1/V2）。

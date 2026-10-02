# Empty Classroom Plan — 本地可运行原型

COMP1110 Group 08 · 空教室可查询系统（学生端只读 + 教师端变更端口）

这套东西是**真能跑起来的**三层应用：Vue 3 + Element Plus（前端）、Spring Boot（后端）、PostgreSQL（数据库）。
技术栈与 Plan §3 一致，界面对应 UI 稿的三屏。

---

## 一键启动 / 停止

```bash
cd ~/Documents/comp1110/项目/empty-classroom

./scripts/start-all.sh     # 数据库 → 后端 → 前端（首次会装依赖，约 3-8 分钟）
./scripts/stop-all.sh      # 全部停掉
```

启动后：

| 入口 | 地址 | 账号 |
|---|---|---|
| 学生端（查空教室，只读） | http://localhost:5173/login | `user1` / `user123` |
| 管理人员端（增/释放教室时间） | http://localhost:5173/manage | `admin1` / `admin123` |
| 后端 API | http://localhost:8080/api/availability?date=2026-10-05&building=CPD&from=14:00&to=15:50 | — |

> 另有一个 `teacher1` / `teacher123` 账号，角色同样是 `admin`（V3 把 `TEACHER` 改名 `ADMIN`，V4 定为小写 `admin`），用来演示"管理员可以管理其他管理员提交的变更"。

## 权限模型：两类用户

| 能力 | `user`（学生） | `admin`（管理人员） |
|---|---|---|
| 查询教室空闲（`/availability`、`/rooms`、`/buildings`） | ✅ | ✅ |
| 提交变更：**Add use**（添加使用）/ **Release**（释放时间）（`POST /updates`） | ❌ 403 | ✅ |
| 撤销变更、释放时间（`DELETE /updates/{id}`） | ❌ 403 | ✅ 可撤销**任何人**提交的 |
| 查看变更（`GET /updates/mine` 我的、`GET /updates` 全部） | ❌ 403 | ✅ 两个都能看 |
| 查看审计日志（`GET /audit`） | ❌ 403 | ✅ |

### 只有两个操作方向

| 操作 | 方向 | 用在 | 效果 |
|---|---|---|---|
| **Add use** | 加占用 | 这段时间**本来没课** | 空闲 → 被占用，学生查不到这间房 |
| **Release** | 减占用 | 这段时间**本来有课** | 从课里挖掉一段，多出可用时间 |

判断标准只有一条：**这段时间原本有没有课**。

（早先版本还有第三个 `CLOSURE`（关闭教室），它和 `Add use` 对可用性的影响完全一样，只是理由不同，所以按 V6 迁移合并进 `Add use`；"理由"照样写在 `reason` 字段里，比如 MB-121 的 "AV repair"。）

**时间以小时为单位**：界面上是整点下拉（09:00、10:00、11:00…），后端也会拒绝非整点（返回 `Times must be on the hour`）。整点对齐让区间运算不会出现 14:30 这种跨界点，`subtract` 出来的碎片也都是整点。

三条设计约束：

1. **学生端根本没有写按钮** —— 不是"点了会报错"，而是界面上不存在这个入口；接口层再拦一道（实测学生 token 调五个写/管理接口全部 403）。
2. **变更不删除，只失效**：撤销是把 `active` 置为 false，记录永久保留，审计里写清"谁撤销了谁提交的"。
3. **管理人员的权力是"管理"，不是"改历史"**：基线课表（`class_slots`）没有修改入口，只能靠临时变更覆盖，而临时变更可以设过期时间自动失效。

单独启停某一层：`scripts/start-db.sh` · `start-backend.sh` · `start-frontend.sh` · `stop-db.sh`

---

## 性能（实测，2026-10-02）

| 指标 | 数值 |
|---|---|
| 教室查询接口吞吐 | **~8,500 请求/秒**（并发 50–1000 都稳定） |
| 单请求延迟 | 5.9 ms @ 50 并发 / 121 ms @ 1000 并发 |
| 每请求 SQL 条数 | **3 条**（房间列表 + 当天课表 + 当天变更） |
| 最大压力实测 | 并发 1000 × 10 秒 → 84,713 次请求全部 200，0 错误 |

三条要点：

1. **原本是 31 条 SQL**（每间房各查一次课表和变更两次，N+1），数据库因此成为瓶颈（~1,100 请求/秒）。改成批量取回、按房间分组后 → 3 条 SQL，吞吐 **7.7 倍**。
2. **连接池不会爆**：并发 1000 时数据库侧只用了 11 条连接。池子耗尽的前提是查询变慢（慢查询占住连接不还），而不是并发高。
3. **没有用 Redis，是有意的**：单实例 + 只读为主 + 数据只有几 KB，进程内就能解决；Redis 的用武之地是多实例共享缓存/限流/会话，那是部署形态问题而不是负载问题。详见 `~/.hermes/knowledge/comp1110-project/performance.md`。

日志：`logs/backend.log` · `logs/frontend.log` · `db/pg.log`

---

## 目录结构

```
empty-classroom/
├── backend/                 Spring Boot（Java 21 / Maven）
│   ├── src/main/java/hku/ec/
│   │   ├── domain/          Room / ClassSlot / RoomUpdate / AuditEntry / AppUser
│   │   ├── repo/            Spring Data JPA 仓储
│   │   ├── service/
│   │   │   ├── AvailabilityService.java   ★ 核心技术：空闲区间计算
│   │   │   ├── UpdateService.java         教师变更 + 到期失效 + 审计
│   │   │   └── AuthService.java           极简登录（原型级）
│   │   └── web/             控制器与 DTO
│   └── src/main/resources/
│       ├── application.yml     缓冲时长、开放时段、数据源
│       └── db/migration/       Flyway：V1 建表、V2 种子数据
├── frontend/                Vue 3 + Vite + Element Plus + Pinia
│   └── src/views/           Login.vue · FindRoom.vue · TeacherUpdates.vue
├── db/pgdata/               本项目专用 PostgreSQL 数据目录（可直接删掉重建）
├── logs/                    运行日志
└── scripts/                 启停脚本
```

---

## 接口（原型版）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/auth/login` | 返回 token（内存保存 24h） |
| GET | `/api/buildings` | 楼栋列表 |
| GET | `/api/rooms?building=CPD` | 教室列表 |
| GET | `/api/availability?date=&building=&from=&minutes=` | **主查询**：哪些教室在指定时段空闲 |
| GET | `/api/rooms/{code}/timeline?date=` | 某教室一天的忙/闲/缓冲片段（画时间轴用） |
| POST | `/api/updates` | 教师提交变更（需 TEACHER） |
| GET | `/api/updates/mine` | 我提交过的变更 |
| DELETE | `/api/updates/{id}` | 撤销变更（只能撤自己的） |
| GET | `/api/audit` | 审计日志（教师可见） |

命令行自测：

```bash
curl -s "http://localhost:8080/api/availability?date=$(date +%F)&building=CPD&from=14:00&minutes=120" | python3 -m json.tool | head -40
```

---

## 这套原型实现了什么（对应 Plan §3）

1. **两套来源合并**：基线课表（`class_slots`，按星期几循环）+ 管理端临时变更（`room_updates`，按具体日期）。
2. **两个操作方向**（都在 `AvailabilityService.busyFrom` 里，20 行纯函数）：
   - `USE` 添加使用 → **加入占用**
   - `RELEASE` 释放时间 → **从占用里挖掉**
3. **换场缓冲**：每个占用段两侧各留 `buffer-minutes`（默认 10 分钟），不足缓冲的间隙不算可用。
4. **空闲区间计算**：合并 → 挖减 → 求补集，只在开放时段（08:00–22:00）内计算。
5. **角色分离**：`user` 只读；`admin`（管理人员）可写；变更写 `audit_log`；变更可设到期时间，到期自动失效。
6. **状态四态**：Available / Free later / In use / No window。**不表示房间空不空**（见"已知限制"）。
7. **只算"有没有被占"，不算"里面有多少人"**：空间是共享的，系统不测人、不预留位。
8. **All rooms 页面**（`/rooms`）：不按时间过滤的全部房间列表；右侧是房间详情（座位数 / 类型 + 当天时间表 + 设施占位 + 学生评价）。
9. **学生评价**：星级 1–5 + 文字，**一人一房一条**（可改可删）；按 `time` 或 `rating` 排序；房间列表直接显示平均分。带 token 时才认得出哪条是自己的，管理员可删任何一条（留审计）。
10. **房间设施（三条）**：座位总数 / 有没有插座 / 座位类型。**只有 admin 能改**（`PATCH /api/rooms/{code}`），每次保存都会盖上"最后核实时间"并写审计；没核实过的房间界面明说 `sample data, not verified yet`。

---

## 已知限制（报告里要如实写）

| 限制 | 说明 |
|---|---|
| 鉴权是原型级 | token 存内存（重启失效）、口令用 SHA-256(salt:password) 而非 bcrypt、无 HTTPS。**不是生产方案**。 |
| 重启后的"假登录" | 后端一重启，旧 token 就失效，但前端因为读 localStorage 仍显示已登录；公开接口（查教室、读评价）不会报错，只是**写操作会失败、评价里认不出"我的"**。演示前重新登录一次即可。 |
| 评价没有审核队列 | 现在是"一人一房一条 + 可自删 + 管理员可删"，够用但没有举报/自动过滤。要不要做取决于组里对 UGC 的口径。 |
| 设施是样例值 | 十条房间的设施是**预填的占位值**（按房间类型给一个合理形状），界面明写 `sample data, not verified yet`；admin 一保存就盖核实时间。**不要把样例值当普查结果写进报告**。 |
| CORS 白名单要跟着新方法走 | 加 `PATCH` 接口时踩过：`WebConfig` 的 `allowedMethods` 里没有 PATCH → 浏览器经 Vite 代理带 `Origin` 头时被 CORS 过滤器挡成 **403 Invalid CORS request**（curl 不带 Origin 所以测不出来）。新增 HTTP 方法时记得同步白名单，并用带 `Origin` 的 curl 复测。 |
| 课表是种子数据 | `V2__seed.sql` 里的排布是演示用，**不是真实课表**；真实数据需要向 Registry 申请或手抄样本。 |
| 学校系统未对接 | 现在是把课表当成"导入"处理；未接入学校中央课表系统的任何接口。 |
| **"空闲"不等于"没人"** | 系统只回答"这间房有没有被课表或征用占住"，**不测房间里有多少人、有没有空位**。教室是多人共用的，学生到了发现人多，系统不负责。界面上已明确写出这一点。 |
| 单实例 | 无集群；定时到期检查是单实例 `@Scheduled`。（吞吐已实测：~8,500 请求/秒，见"性能"一节） |
| 未做 | **座位级占用 / 房间空不空**、学生"报错"反馈、移动端适配、i18n。 |

---

## 自动化测试（对应 Plan §3.4 的验证设计）

```bash
./scripts/test.sh          # 需要数据库在跑；脚本会先确保库起来
```

当前结果：**12 个用例全部通过**（2 个测试类）。

| Plan §3.4 里的算例 | 对应测试方法 | 结果 |
|---|---|---|
| 正常间隙（两节课之间的大空档可用） | `normalGapIsUsable` | ✓ |
| 零间隙（背靠背两节课，中间不算可用） | `backToBackGivesNoWindow` | ✓ |
| 间隙小于换场缓冲（短间隙不展示为可用） | `gapShorterThanBufferIsNotOffered` | ✓ |
| 某节课被取消（时间被释放） | `cancelledClassReleasesTime` | ✓ |
| 征用撞上原本空闲的时段 | `requisitionBlocksFreeTime` | ✓ |
| 查询时刻正在上课 | `duringClassIsInUse` | ✓ |

底层区间运算另有 6 个纯函数测试（`AvailabilityMathTest`）：合并、整段挖减、中间挖减、不相交挖减、开放时段内求补集、全天无占用。

测试用 `@Transactional`，跑完自动回滚，**不会污染演示数据**。

---

## 依赖与端口

| 组件 | 版本 | 端口 |
|---|---|---|
| PostgreSQL | 17.11（Homebrew，数据目录在项目内） | **5433**（避开系统默认 5432） |
| 后端 | Spring Boot 4.1.1 / Java 21（LTS） | 8080 |
| 前端 | Vite dev server | 5173 |
| 浏览器访问 | 走 Vite proxy，无需配 CORS | — |

macOS 上从零复现需要：`brew install maven postgresql@17 openjdk@21`（本机已装）。

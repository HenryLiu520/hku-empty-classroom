# 在自己机器上跑起来

这一页的每条命令都在**做出这个项目的那台机器**上跑过，输出核对过再写进来。

| | 用的版本 |
|---|---|
| JDK | 21（脚本会自动找；Java 26 也能跑） |
| Maven | 3.10 |
| Node | 26（18 以上就够） |
| PostgreSQL | 17.11，二进制在 `PATH` 里或装在 Homebrew 下 |

**不需要** Docker、Redis 或任何云账号。数据库是项目文件夹里的一个私有实例，不动你机器上已有的服务。

---

## 1. 一条命令

```bash
git clone https://github.com/HenryLiu520/hku-empty-classroom.git
cd hku-empty-classroom
./scripts/start-all.sh
```

然后打开 **<http://localhost:5173/login>**，用 `user1` / `user123` 登录。

`start-all.sh` 会把起好的地址打出来：

```
Student view : http://localhost:5173/login
Admin port   : http://localhost:5173/manage
API          : http://localhost:8080/api/availability?date=…&building=Central%20Podium&from=14:00&to=15:50
```

全部停掉：`./scripts/stop-all.sh`。

---

## 2. 这条命令做了什么

```
start-db.sh        首次 initdb → 集群在 db/pgdata → 端口 5433 → createdb emptyclassroom
start-backend.sh   mvn spring-boot:run → 端口 8080 → Flyway 跑 V1…V16 → 建表 + 种子数据
start-frontend.sh  首次 npm install → Vite → 端口 5173
```

后端是**等 `http://localhost:8080/api/buildings` 有响应**才继续的，所以打印出来的 "up" 是真的起来了，不是睡够秒数。

---

## 3. 一步一步来

想看清每一层怎么起来的，或者一键脚本卡住了，用这一段。

### 3.1 数据库

```bash
./scripts/start-db.sh
```

预期输出：

```
[db] first run: initdb into …/db/pgdata
[db] ready: postgresql://<你>@localhost:5433/emptyclassroom
```

检查：

```bash
psql -h 127.0.0.1 -p 5433 -U "$USER" -d emptyclassroom -c '\dt'
```

应该看到 `rooms`、`class_slots`、`app_user`、`room_updates`、`room_reviews`、`review_photos`、`audit_log`、`flyway_schema_history`。

> 表要等后端启动过**才有** —— 是 Flyway 建的。所以这一步 `\dt` 空着是正常的，接着看 3.2。

### 3.2 后端

```bash
./scripts/start-backend.sh          # 这个终端别关
```

等日志里出现 `Started EcApplication`，另开一个终端：

```bash
curl -s http://localhost:8080/api/about
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/api/buildings
```

预期：先一段 JSON，然后 `200`。

### 3.3 桌面端

```bash
./scripts/start-frontend.sh
```

第一次会装依赖（约 30 秒），然后打印 `Local: http://localhost:5173/`。

### 3.4 手机端（可选）

```bash
cd mobile
npm install
npm run dev          # http://localhost:5174
```

它通过开发服务器的代理打同一套 `/api`，所以**后端不用改**，也没有第二套接口要同步。管理员页面只在桌面端。

### 3.5 模拟学校系统（可选）

一个独立的 Spring Boot 服务，带**自己的**数据库（`mockschool`），用来测"课表来自别处"的情况。

```bash
./scripts/mock-school.sh            # http://127.0.0.1:9000（自带控制台在 /）
./scripts/verify-mock.sh            # 10 项端到端检查
```

控制台可以把数据标成过期、变慢、挂掉，也能直接手工改课表。

---

## 4. 账号

| 账号 | 密码 | 角色 | 能做什么 |
|---|---|---|---|
| `user1` / `user2` | `user123` | 学生 | 浏览教室、写和读自己的评价 |
| `admin1` / `teacher1` | `admin123` | 管理员 | 锁定/释放教室时间、改设施、撤销自己提交的变更 |
| `super1` / `super2` | `super123` | 超级管理员 | 上面全部 + 管账户 + 删任何人的评价 |

都是迁移脚本创建的**演示账号**，不是密钥；这个仓库里没有任何真实用户数据。

---

## 5. 数据放在哪

| | 位置 | 在 git 里吗 |
|---|---|---|
| 数据库集群（71 MB） | `db/pgdata/` | ❌ `.gitignore` 里是 `/db/` |
| 表结构 + 种子 | `backend/src/main/resources/db/migration/V1…V16` | ✅ |
| 模拟系统的表结构 + 种子 | `mock-school-api/src/main/resources/db/migration/` | ✅ |
| 评价照片 | `backend/uploads/review-photos/` | ❌ |

数据目录是二进制的、跟某一个 PostgreSQL 版本绑定，不能搬；**迁移脚本才是可复现的部分** ——
任何机器跑起后端，都会得到同样的表、10 间教室、100 节课、6 个账号、6 条评价。

---

## 6. 测试

```bash
bash scripts/test.sh
```

预期结尾：

```
Tests run: 31, Failures: 0, Errors: 0, Skipped: 0
```

---

## 7. 端口

| 端口 | 是什么 | 谁需要 |
|---|---|---|
| 5433 | PostgreSQL（项目私有） | 全部 |
| 8080 | Spring Boot 接口 | 两个客户端 |
| 5173 | 桌面端（Vite） | 主应用 |
| 5174 | 手机端（Vite） | 可选 |
| 9000 | 模拟学校系统 | 可选 |

`stop-all.sh` 会把 5433、8080、5173、5174、9000 一起停掉。

---

## 8. 常见问题

**`Address already in use`（5173 / 5174 / 8080 / 9000）**

```bash
lsof -nP -iTCP:5173 -sTCP:LISTEN     # 看是谁
kill <pid>                            # 或者 ./scripts/stop-all.sh
```

如果你还有别的 Vite 项目在跑，5174 会被悄悄顶到 5175，然后你看着错误的页面调半天。这个项目把端口**钉死**（`strictPort: true`），端口被占会直接报错，不会偷偷换。

**`FATAL: role "…" does not exist`**

集群是别的用户建的。`start-db.sh` 用的是 `initdb -U $USER`，所以角色名就是你的 shell 用户名。用那个用户连，或者从干净的集群重来：

```bash
./scripts/stop-db.sh && rm -rf db/pgdata && ./scripts/start-db.sh
```

**`initdb: command not found`**

PostgreSQL 17 没装，或者不在 `PATH`。macOS：`brew install postgresql@17`；Debian/Ubuntu：`apt install postgresql-17`。装完 `initdb --version` 验一下。

**后端起来了，但 `\dt` 是空的**

Flyway 没跑。看 `logs/backend.log` 里有没有 `Migrating schema`。注意 Spring Boot 4 里 Flyway 要 `spring-boot-starter-flyway`（只有 `flyway-core` 不够）—— 这个项目已经用了 starter，所以表是空的一般意味着后端根本没启动完。

**`Validate failed: Migration checksum mismatch`**

有人改了**已经执行过**的迁移。不要改跑过的迁移，新加一个。想推倒重来：`./scripts/stop-db.sh && rm -rf db/pgdata && ./scripts/start-db.sh`。

**时间窗被 400 拒绝**

规则是整块：`from` 必须是整点，`to` 必须以 `:50` 结尾（`09:00–10:50`、`13:00–15:50`）。跟课表重叠的时段不能占，而且**学校课表永远压过管理员**。

**周四打开全是空教室**

已经用 `V16__thursday_slots.sql` 修了 —— 最早的种子只有周一、周二、周三、周五。如果你的库是旧的，重跑迁移或重建集群。

---

## 9. 在全新 clone 上验证过

把仓库 clone 到一个空目录，三块都编一遍：

```bash
git clone https://github.com/HenryLiu520/hku-empty-classroom.git /tmp/fresh-clone
cd /tmp/fresh-clone/backend && mvn -o -q -DskipTests package    # → target/empty-classroom-0.1.0.jar（52 MB）
cd /tmp/fresh-clone/mobile  && npm ci && npm run build          # → 161 KB JS、4 KB CSS
cd /tmp/fresh-clone/frontend && npm ci && npm run build         # → 干净构建
```

三块都过了，说明仓库里**没有漏掉任何构建需要的东西**。

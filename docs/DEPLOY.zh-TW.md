# 在自己機器上跑起來

這一頁的每條命令都在**做出這個專案的那臺機器**上跑過，輸出核對過再寫進來。

| | 用的版本 |
|---|---|
| JDK | 21（腳本會自動找；Java 26 也能跑） |
| Maven | 3.10 |
| Node | 26（18 以上就夠） |
| PostgreSQL | 17.11，二進位制在 `PATH` 裡或裝在 Homebrew 下 |

**不需要** Docker、Redis 或任何雲端帳號。資料庫是專案資料夾裡的一個私有實例，不動你機器上已有的服務。

---

## 1. 一條命令

```bash
git clone https://github.com/HenryLiu520/hku-empty-classroom.git
cd hku-empty-classroom
./scripts/start-all.sh
```

然後開啟 **<http://localhost:5173/login>**，用 `user1` / `user123` 登入。

`start-all.sh` 會把起好的地址打出來：

```
Student view : http://localhost:5173/login
Admin port   : http://localhost:5173/manage
API          : http://localhost:8080/api/availability?date=…&building=Central%20Podium&from=14:00&to=15:50
```

全部停掉：`./scripts/stop-all.sh`。

---

## 2. 這條命令做了什麼

```
start-db.sh        首次 initdb → 叢集在 db/pgdata → 連接埠 5433 → createdb emptyclassroom
start-backend.sh   mvn spring-boot:run → 連接埠 8080 → Flyway 跑 V1…V16 → 建表 + 種子資料
start-frontend.sh  首次 npm install → Vite → 連接埠 5173
```

後端是**等 `http://localhost:8080/api/buildings` 有響應**才繼續的，所以列印出來的 "up" 是真的起來了，不是睡夠秒數。

---

## 3. 一步一步來

想看清每一層怎麼起來的，或者一鍵腳本卡住了，用這一段。

### 3.1 資料庫

```bash
./scripts/start-db.sh
```

預期輸出：

```
[db] first run: initdb into …/db/pgdata
[db] ready: postgresql://<你>@localhost:5433/emptyclassroom
```

檢查：

```bash
psql -h 127.0.0.1 -p 5433 -U "$USER" -d emptyclassroom -c '\dt'
```

應該看到 `rooms`、`class_slots`、`app_user`、`room_updates`、`room_reviews`、`review_photos`、`audit_log`、`flyway_schema_history`。

> 表要等後端啟動過**才有** —— 是 Flyway 建的。所以這一步 `\dt` 空著是正常的，接著看 3.2。

### 3.2 後端

```bash
./scripts/start-backend.sh          # 這個終端別關
```

等日誌裡出現 `Started EcApplication`，另開一個終端：

```bash
curl -s http://localhost:8080/api/about
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/api/buildings
```

預期：先一段 JSON，然後 `200`。

### 3.3 桌面端

```bash
./scripts/start-frontend.sh
```

第一次會裝依賴（約 30 秒），然後列印 `Local: http://localhost:5173/`。

### 3.4 手機端（可選）

```bash
cd mobile
npm install
npm run dev          # http://localhost:5174
```

它透過開發伺服器的代理打同一套 `/api`，所以**後端不用改**，也沒有第二套 API要同步。管理員頁面只在桌面端。

### 3.5 模擬學校系統（可選）

一個獨立的 Spring Boot 服務，帶**自己的**資料庫（`mockschool`），用來測"課表來自別處"的情況。

```bash
./scripts/mock-school.sh            # http://127.0.0.1:9000（自帶控制檯在 /）
./scripts/verify-mock.sh            # 10 項端到端檢查
```

控制檯可以把資料標成過期、變慢、掛掉，也能直接手工改課表。

---

## 4. 帳號

| 帳號 | 密碼 | 角色 | 能做什麼 |
|---|---|---|---|
| `user1` / `user2` | `user123` | 學生 | 瀏覽教室、寫和讀自己的評價 |
| `admin1` / `teacher1` | `admin123` | 管理員 | 鎖定/釋放教室時間、改設施、撤銷自己提交的變更 |
| `super1` / `super2` | `super123` | 超級管理員 | 上面全部 + 管賬戶 + 刪任何人的評價 |

都是遷移腳本建立的**演示帳號**，不是金鑰；這個倉庫裡沒有任何真實使用者資料。

---

## 5. 資料放在哪

| | 位置 | 在 git 裡嗎 |
|---|---|---|
| 資料庫叢集（71 MB） | `db/pgdata/` | ❌ `.gitignore` 裡是 `/db/` |
| 表結構 + 種子 | `backend/src/main/resources/db/migration/V1…V16` | ✅ |
| 模擬系統的表結構 + 種子 | `mock-school-api/src/main/resources/db/migration/` | ✅ |
| 評價照片 | `backend/uploads/review-photos/` | ❌ |

資料目錄是二進位制的、跟某一個 PostgreSQL 版本繫結，不能搬；**遷移腳本才是可復現的部分** ——
任何機器跑起後端，都會得到同樣的表、10 間教室、100 節課、6 個帳號、6 條評價。

---

## 6. 測試

```bash
bash scripts/test.sh
```

預期結尾：

```
Tests run: 31, Failures: 0, Errors: 0, Skipped: 0
```

---

## 7. 連接埠

| 連接埠 | 是什麼 | 誰需要 |
|---|---|---|
| 5433 | PostgreSQL（專案私有） | 全部 |
| 8080 | Spring Boot 介面 | 兩個客戶端 |
| 5173 | 桌面端（Vite） | 主應用 |
| 5174 | 手機端（Vite） | 可選 |
| 9000 | 模擬學校系統 | 可選 |

`stop-all.sh` 會把 5433、8080、5173、5174、9000 一起停掉。

---

## 8. 常見問題

**`Address already in use`（5173 / 5174 / 8080 / 9000）**

```bash
lsof -nP -iTCP:5173 -sTCP:LISTEN     # 看是誰
kill <pid>                            # 或者 ./scripts/stop-all.sh
```

如果你還有別的 Vite 專案在跑，5174 會被悄悄頂到 5175，然後你看著錯誤的頁面調半天。這個專案把連接埠**釘死**（`strictPort: true`），連接埠被佔會直接報錯，不會偷偷換。

**`FATAL: role "…" does not exist`**

叢集是別的使用者建的。`start-db.sh` 用的是 `initdb -U $USER`，所以角色名就是你的 shell 使用者名稱。用那個使用者連，或者從乾淨的叢集重來：

```bash
./scripts/stop-db.sh && rm -rf db/pgdata && ./scripts/start-db.sh
```

**`initdb: command not found`**

PostgreSQL 17 沒裝，或者不在 `PATH`。macOS：`brew install postgresql@17`；Debian/Ubuntu：`apt install postgresql-17`。裝完 `initdb --version` 驗一下。

**後端起來了，但 `\dt` 是空的**

Flyway 沒跑。看 `logs/backend.log` 裡有沒有 `Migrating schema`。注意 Spring Boot 4 裡 Flyway 要 `spring-boot-starter-flyway`（只有 `flyway-core` 不夠）—— 這個專案已經用了 starter，所以表是空的一般意味著後端根本沒啟動完。

**`Validate failed: Migration checksum mismatch`**

有人改了**已經執行過**的遷移。不要改跑過的遷移，新加一個。想推倒重來：`./scripts/stop-db.sh && rm -rf db/pgdata && ./scripts/start-db.sh`。

**時間窗被 400 拒絕**

規則是整塊：`from` 必須是整點，`to` 必須以 `:50` 結尾（`09:00–10:50`、`13:00–15:50`）。跟課表重疊的時段不能佔，而且**學校課表永遠壓過管理員**。

**週四開啟全是空教室**

已經用 `V16__thursday_slots.sql` 修了 —— 最早的種子只有週一、週二、週三、週五。如果你的庫是舊的，重跑遷移或重建叢集。

---

## 9. 在全新 clone 上驗證過

把倉庫 clone 到一個空目錄，三塊都編一遍：

```bash
git clone https://github.com/HenryLiu520/hku-empty-classroom.git /tmp/fresh-clone
cd /tmp/fresh-clone/backend && mvn -o -q -DskipTests package    # → target/empty-classroom-0.1.0.jar（52 MB）
cd /tmp/fresh-clone/mobile  && npm ci && npm run build          # → 161 KB JS、4 KB CSS
cd /tmp/fresh-clone/frontend && npm ci && npm run build         # → 乾淨建置
```

三塊都通過了，說明倉庫裡**沒有漏掉任何建置需要的東西**。

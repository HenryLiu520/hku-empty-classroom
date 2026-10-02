# Empty Classroom Plan

[English](README.md) · [简体中文](README.zh-CN.md) · **繁體中文**

> 回答一個關於港大教室的問題：**課堂之間，到底哪幾間教室是空的？**
> Vue 3 + Spring Boot + PostgreSQL · COMP1110 第 08 組（香港大學）

![查詢頁：14:00 到 15:50 哪些教室空著](docs/images/find-a-room.png)

---

## 這個專案解決什麼問題

09:50 到 10:10 之間，港大每一棟教學大樓的走廊裡都擠著找座位的同學。他們問的是同一個問題——**附近有沒有空教室？**——但沒有人能在不走過去看看的情況下回答它。

回答這個問題需要的資訊其實一直都在。學校有每週課表，而且它已經把資料推到了最末端：**每間教室門外都有一個顯示屏，顯示這間教室當天的課表**。缺的不是資料，而是一個把課表變成「**接下來這一小時哪幾間教室空著**」的視圖，給一個正站在走廊裡的學生看。

本專案把決定「教室能不能用」的兩件事合起來——**固定的週課表** 與 **管理人員發布的臨時變更**（教室被活動佔用、或某段不再使用）——然後報出剩下的空檔。它刻意只描述**房間，不描述人**：不猜房間裡有幾個人，也不要求學生預約任何東西。

## 功能

- **查詢** —— 選日期、樓棟、時間窗（`14:00 → 15:50`），列出整段都空著的教室；每條結果標明「現在可用 / 稍後可用 / 正在使用」。
- **每間教室有自己的頁面** —— 當天課表畫成**以整點為格、以整塊為單位的方格**（兩小時的課就是跨兩格的一整塊，標籤寫真實時間 `13:00–14:50`）、設施（座位數、插座、座位類型）、以及其他同學的評價。
- **All rooms** —— 不按時間過濾的完整教室列表，可排序。
- **評價** —— 一人一房一條星級；管理員可刪任何一條，刪除留審計。
- **設施編輯** —— 座位數／插座／座位類型**只有管理員能改**；每次儲存蓋上「最後核實時間」，從未核實過的房間在介面上明確標註。
- **管理端變更** —— 管理員可以**新增使用（add use）**或**釋放時間（release）**，每次變更都有到期時間與審計記錄。
- **三種語言** —— 整個介面可在頂欄（和登入頁）切換 英文 / 簡體中文 / 繁體中文，選擇會被記住。
- **註冊** —— 任何 `hku.hk` 後綴的信箱（`@connect.hku.hk`、`@hku.hk` 等）都可以用信箱＋密碼註冊學生帳號。原型階段**不發驗證信**。

## 介面

| | |
|---|---|
| **按時間窗查詢** | **教室詳情頁** |
| ![查詢](docs/images/find-a-room.png) | ![教室頁](docs/images/room-page.png) |
| **All rooms** | **管理端變更** |
| ![全部教室](docs/images/all-rooms.png) | ![管理端](docs/images/admin-change.png) |
| **設施編輯（僅管理員）** | |
| ![設施](docs/images/facilities.png) | |

## 怎麼算的

核心一句話：**空檔 = 一天 減去 被佔的塊。**

把一天畫成 08:00 到 22:00 的一條線，每節課、每個發布的變更都是一個塗黑的塊；**沒被塗黑的部分就是空檔**。「10:00 到 11:00 這間房空嗎？」就變成「這一段是否整段落在沒塗黑的地方」。後端做的就是把这些時間塊**合併與相減**，並且每個塊兩側各留 **10 分鐘換場餘量**——因為不能上課上到最後一分鐘才算空出來。

介面上這件事畫成**一格一小時的方格**：一節 10:00–11:50 的課會佔滿 10:00 和 11:00 兩格，格子裡寫的是真實時間（`11:00–11:50`）——**格線對齊整點，數字照樣是幾點 50**。

而這些「被佔的塊」從哪來，才是最關鍵的部分：

| | |
|---|---|
| **佔用以整塊為單位** | 所有課整點開始、`:50` 結束，所以兩小時的課佔兩個整塊（`13:00–14:50`），絕不會佔半塊；管理員的變更同樣如此：起點整點、終點 `:50`。 |
| **學校課表優先級最高** | 管理人員發布的變更，只能佔用**課表顯示為空**的時間。任何與課重疊的變更會被伺服器端**直接拒絕**（`400 TIMETABLE_PRIORITY`），而不是默默接受一個不生效的變更。 |
| **學生影響不了佔用** | 學生帳號根本不存在寫入路徑。攔截在**伺服器端**做，不是靠藏按鈕：學生 token 呼叫變更介面一律 `403`。 |
| **我們是去問，不是抄一份** | 學校課表從對方的**唯讀介面**讀取、定期輪詢，並用 `last-updated` 時間戳做新鮮度檢查。我們不儲存副本，絕不寫對方系統。原型階段這個來源仍是樣本資料（見「限制」）。 |

### 伺服器端真正執行的規則

| 規則 | 實作位置 | 怎麼驗證的 |
|---|---|---|
| 起點整點、終點 `:50`（查詢與管理員變更同規） | `AvailabilityService.requireSearchWindow` / `requireWholeHours` | `from=14:30` → `400`；`to=16:00` → `400` |
| 管理員不得觸碰課表裡的課 | `UpdateService.create`（重疊判斷） | 對課程時段發 `RELEASE`/`USE` → `400 TIMETABLE_PRIORITY` |
| 只有管理員能發布變更 | `AuthService.require(token, "admin")` | 學生 token → `403 FORBIDDEN` |
| 只有 `hku.hk` 信箱能註冊 | `AuthService.isHkuEmail` | `@gmail.com`、`@connect.hku.hk.evil.com` → `400` |
| 每次變更可追溯、可到期失效 | `room_updates`、`audit_log` | `GET /api/audit` |

## 架構

![架構：一個 Vue 應用兩種帳號角色、一個服務、一個資料庫，學校課表是唯讀輸入](docs/images/architecture.png)

三個部分，**它們之間的分工就是設計本身**：

1. **一個 Vue 3 單頁應用，兩種帳號角色。** 學生頁面裡不存在任何寫入控件；管理員頁面是同一個應用外加若干路由，**能做什麼由伺服器端決定**。
2. **一個 Spring Boot 服務**：計算空檔、執行上面的規則，是系統裡**唯一會寫資料的部分**。
3. **一個 PostgreSQL 資料庫**：課表、發布的變更、審計日誌、帳號。

學校課表是**輸入，不是我們要造的東西**。我們的服務按計畫呼叫對方的唯讀介面並比對 `last-updated` 時間戳；資料變舊就說變舊，不假裝是當前的。

## 快速開始

**環境需求：** Java 21、Maven、Node 18+、PostgreSQL 17（macOS 上 `brew install maven openjdk@21 postgresql@17 node`）。

```bash
git clone https://github.com/HenryLiu520/hku-empty-classroom.git
cd hku-empty-classroom

./scripts/start-all.sh      # 依序啟動 PostgreSQL(5433) → Spring Boot(8080) → Vite(5173)
# 首次執行會安裝依賴並套用 Flyway 遷移，約 3–8 分鐘
```

然後打開 **http://localhost:5173/login** 登入：

| 帳號 | 角色 | 能看到什麼 |
|---|---|---|
| `user1` | `user`（學生） | 只有查詢與教室頁——介面上不存在任何寫入入口 |
| `admin1` | `admin` | 同一個應用，外加 *Change room use time*、設施編輯、審計 |
| `teacher1` | `admin` | 第二個管理員，用來示範「管理員可以管理別的管理員提交的變更」 |

種子密碼在 `backend/src/main/resources/db/migration/V2__seed.sql`（帳號名在 `V5__account_names.sql` 中改過）。

停止：`./scripts/stop-all.sh`。單獨啟停某一層：`scripts/start-db.sh`、`start-backend.sh`、`start-frontend.sh`。

命令列自測：

```bash
curl -s "http://localhost:8080/api/availability?date=$(date +%F)&building=CPD&from=14:00&to=15:50" | python3 -m json.tool | head -30
```

## 介面（API，原型版）

| 方法 | 路徑 | 說明 |
|---|---|---|
| `POST` | `/api/auth/register` | 用 `hku.hk` 信箱註冊，回傳 token |
| `POST` | `/api/auth/login` | 回傳 token（記憶體保存 24 小時） |
| `GET` | `/api/buildings` | 樓棟列表 |
| `GET` | `/api/rooms?building=CPD` | 教室列表（含設施與評價均分） |
| `GET` | `/api/availability?date=&building=&from=&to=` | **主查詢**：整段都空著的教室 |
| `GET` | `/api/rooms/{code}/timeline?date=` | 某教室一天的忙／閒方格 |
| `POST` | `/api/updates` | 發布變更（`USE` / `RELEASE`）——僅管理員 |
| `GET` | `/api/updates/mine`、`GET`/`DELETE` `/api/updates[/{id}]` | 檢視與撤銷變更（撤銷保留記錄，只設為失效） |
| `PATCH` | `/api/rooms/{code}` | 編輯設施——僅管理員 |
| `GET` | `/api/rooms/{code}/reviews`、`POST`、`DELETE /api/reviews/{id}` | 評價（一人一房一條） |
| `GET` | `/api/audit` | 審計日誌——僅管理員 |

## 測試

```bash
./scripts/test.sh        # 需要資料庫在跑；腳本會先確保資料庫起來
```

**21 個自動化用例全部通過**（9 個區間運算純函式測試 + 10 個可用性／規則測試 + 2 個註冊規則測試）。報告裡會引用的邊界算例：

| 算例 | 測試方法 | |
|---|---|---|
| 兩節課之間的大空檔可用 | `normalGapIsUsable` | ✓ |
| 背靠背兩節課，中間不給窗口 | `backToBackGivesNoWindow` | ✓ |
| 間隙小於換場餘量，不展示為可用 | `gapShorterThanBufferIsNotOffered` | ✓ |
| 釋放只能撤銷管理員自己加的佔用 | `releaseOnlyCancelsAnAdminAddition` | ✓ |
| 釋放永遠挖不掉課表裡的課 | `releaseCannotOverrideTheTimetable` | ✓ |
| 與課重疊的變更被直接拒絕 | `adminCannotChangeClassTime` | ✓ |
| 查詢時刻正在上課 → 正在使用 | `duringClassIsInUse` | ✓ |
| 非整點時間被拒絕 | `offHourTimesAreRejected` | ✓ |
| 兩小時的課是一整塊（跨兩格） | `timelineMergesWholeHourBlocks` | ✓ |
| 只有 `hku.hk` 信箱能註冊 | `RegistrationRulesTest`（2 例） | ✓ |

測試用 `@Transactional`，跑完自動回滾，**不會污染示範資料**。

## 效能（實測，2026-10-02）

| 指標 | 數值 |
|---|---|
| 查詢介面吞吐 | **約 8,500 請求/秒**（並發 50–1000 都穩定） |
| 單請求延遲 | 5.9 ms @ 50 並發 · 121 ms @ 1000 並發 |
| 每請求 SQL 條數 | **3 條**（房間列表 + 當天課表 + 當天變更） |
| 最大壓力實測 | 並發 1000 × 10 秒 → 84,713 次請求全部 `200`，0 錯誤 |

第一版每請求要發 **31 條 SQL**（每間房各查一次課表與變更，典型的 N+1），資料庫因此成為瓶頸（約 1,100 請求/秒）。改成批次取回、在記憶體裡按房間分組後降到 3 條，吞吐提升 **7.7 倍**。**沒有用 Redis 是有意的**：單實例 + 以唯讀為主 + 資料只有幾 KB；Redis 的價值在多實例部署，而不是這個負載。

## 目錄結構

```
empty-classroom/
├── backend/                      Spring Boot 4.1.1 · Java 21 · Maven
│   └── src/main/java/hku/ec/
│       ├── domain/               Room · ClassSlot · RoomUpdate · AuditEntry · AppUser
│       ├── repo/                 Spring Data JPA 儲存庫
│       ├── service/
│       │   ├── AvailabilityService.java   ★ 空檔區間計算
│       │   ├── UpdateService.java         變更、到期失效、課表優先校驗
│       │   └── AuthService.java           角色、註冊規則、原型級 token
│       └── web/                  控制器與 DTO
│   └── src/main/resources/
│       ├── application.yml       換場餘量、開放時段、資料來源
│       └── db/migration/         Flyway V1…V9（建表、種子、評價、設施、註冊）
├── frontend/                     Vue 3 · Vite · Element Plus · Pinia
│   └── src/views/                Login.vue · FindRoom.vue · AllRooms.vue（含房間詳情面板）· AdminUpdates.vue
├── docs/images/                  上面用到的截圖
├── db/pgdata/                    專案專用的 PostgreSQL 資料目錄（可刪掉重建）
├── logs/                         backend.log · frontend.log · pg.log
└── scripts/                      start-all.sh · stop-all.sh · test.sh · 各層單獨腳本
```

## 已知限制（報告裡要如實寫）

| 限制 | 說明 |
|---|---|
| **「空檔」不等於「沒人」** | 系統回答的是「這間房有沒有被課表或發布的變更佔住」，**不測房間裡有幾個人**。教室是共用的，介面上已明確寫出。 |
| 課表是樣本資料 | `V2__seed.sql` 是合理的模擬排布，**不是真實課表**；學校介面尚未接入。報告裡任何數字都必須來自真實資料再聲稱。 |
| 鑑權是原型級 | token 存記憶體（重啟失效）—— 失效時應用會**自動登出並回到登入頁**；密碼用 `SHA-256(salt:password)` 而非 bcrypt、無 HTTPS。**不是生產方案**。 |
| 沒有信箱驗證 | 註冊只檢查後綴，任何人可以填一個形如 `hku.hk` 的地址。 |
| 評價沒有審核佇列 | 「一人一房一條 + 可自刪 + 管理員可刪」，沒有檢舉或自動過濾。 |
| 設施是樣本值 | 按房間類型預填的佔位值；管理員儲存前介面標註 `sample data, not verified yet`。**不要當普查結果寫進報告**。 |
| 單實例 | 無叢集；到期檢查是單實例 `@Scheduled`。 |
| CORS 白名單要跟著方法走 | 新增 HTTP 方法時 `WebConfig.allowedMethods` 也要同步——漏掉 `PATCH` 曾導致瀏覽器側 `403 Invalid CORS request`，而 `curl`（不帶 `Origin`）依然通過。 |

## 授權與致謝

本儲存庫是課程作業、不對外分發，因此沒有獨立的 LICENSE 檔案。用到的第三方元件全部是寬鬆授權——前端棧（Vue 3、Vite、Element Plus、Pinia、axios、vue-i18n）為 MIT，Spring Boot 與 Flyway 為 Apache-2.0，資料庫為 PostgreSQL 授權，JDBC 驅動為 BSD-2-Clause——逐項列在 [THIRD-PARTY.md](THIRD-PARTY.md)。介面沿用了 youlai [vue3-element-admin](https://github.com/youlaitech/vue3-element-admin)（MIT）帶起的側欄式後台版式；**未複製該專案任何程式碼**。

## 專案背景

COMP1110 Group Project，第 08 組 —— 香港大學，2026。按課程《Group Project Guidelines》，程式碼與原型可選、不計分：這個原型存在的意義是證明我們 Project Plan 裡的設計**確實能跑起來**。

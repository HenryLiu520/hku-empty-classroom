# 数据库结构

两个库，一个服务一个。两张 ER 图都是**从跑着的数据库读出来的**（列、主键、约束直接查 PostgreSQL），不是照着迁移文件画的 —— 图上就是系统真实的样子。

---

## 1. 项目库 —— `emptyclassroom`

![ER 图](diagrams/er-main.png)

七张表。`flyway_schema_history` 故意没画：那是 Flyway 自己的记账，不属于模型。

| 表 | 存什么 | 现在多少行 |
|---|---|---|
| `rooms` | 每间教室一行；`code` 唯一 | 10 |
| `class_slots` | 学校课表：哪间房、周几、哪个时段、哪门课 | 100 |
| `room_updates` | 管理员发布的变更（占用 / 释放），带原因和可选过期时间 | 7 |
| `room_reviews` | 每人每间房一条评价 | 6 |
| `review_photos` | 评价带的照片；**文件在磁盘，这张表只存指针** | 2 |
| `app_user` | 账号与角色（`user` / `admin` / `superadmin`） | 6 |
| `audit_log` | 只追加的变更记录 | 101 |

**空闲是怎么算出来的。** 没有"空闲教室"这张表。空闲是**算**出来的：把 `class_slots`（学校课表）加上生效中的 `room_updates`（员工加进去或释放掉的）从教学日里减掉，剩下的就是接口回答的内容。所以库里没有预约表 —— 这个系统里没有任何东西能"占用"一间房。

### 图上特意画出来的几件事

- **`room_reviews.created_by`、`room_updates.created_by`、`audit_log.actor` 存的是用户名文本，没有外键。** 这是真实的取舍：账号删掉后，评价会指向一个不存在的名字。换成级联外键则会连评价一起删掉 —— 对评价系统更糟。写在这里而不是藏起来。
- **`(room_id, created_by)` 是**联合**唯一**，不是各自唯一：每人每间房只能一条评价。图上 UQ 标在 `created_by`，真正生效的是这两列一起。
- **`audit_log` 不引用任何表。** `entity` + `entity_id` 是刻意的多态引用，这样被记录的那行删掉了，日志还留着。
- **`class_slots` 和 `room_updates` 都有 `CHECK (end_time > start_time)`** —— 就算服务层有 bug，数据库也拒绝反着来的时间块。
- **`review_photos.byte_size`** 让服务不碰文件就能给出 `Content-Length`；`file_name` 唯一，因为文件名是服务端生成的（UUID + 扩展名），从不采用上传方的名字。

---

## 2. 模拟学校系统 —— `mockschool`

![ER 图（模拟系统）](diagrams/er-mock.png)

三张表，在**另一个 PostgreSQL 库**里 —— 两个系统不共享存储，只共享 HTTP 契约。

| 表 | 存什么 |
|---|---|
| `rooms` | 这份数据发布的教室 |
| `class_slots` | 发布的课表 |
| `feed_meta` | 只有一行（`CHECK id = 1`）：`provider`、`term`、`generated_at`、`revision` |

`feed_meta` 是整个服务的关键。`generated_at` 变成接口里的 `lastUpdated`，客户端由此知道答案有多旧；`revision` 每次写入加一，客户端轮询一个小接口就能判断要不要拉全量课表。

---

## 3. 刻意没有建模的东西

| 没有 | 为什么 |
|---|---|
| `students` 表 | 学生身份学校已经有了；复制一份等于多存一份本项目没有理由持有的个人数据 |
| 预约 / 占座表 | 学生不订房，只是**查**。只有员工发布变更，那就是 `room_updates` |
| `buildings` 表 | 楼名现在是房间上的一个字符串，还没有任何"按楼"的属性要存；单独建表只是一次没意义的 join |
| 教室的实时占用 | 项目回答的是课表，不是传感器。加这一列会暗示一个并不存在的数据源 |

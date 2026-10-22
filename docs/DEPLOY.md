# Run it on your own machine

Everything on this page was run on the machine that built the project. If a command is here, it
was executed and its output checked.

| | Version used |
|---|---|
| JDK | 21 (the scripts pick it up; Java 26 also runs it) |
| Maven | 3.10 |
| Node | 26 (18 or newer is enough) |
| PostgreSQL | 17.11, binaries on `PATH` or under Homebrew |

You do **not** need Docker, Redis, or a cloud account. The database is a private cluster inside
the project folder, so nothing on your machine is touched.

---

## 1. Fast path

```bash
git clone https://github.com/HenryLiu520/hku-empty-classroom.git
cd hku-empty-classroom
./scripts/start-all.sh
```

Then open **<http://localhost:5173/login>** and sign in with `user1` / `user123`.

`start-all.sh` prints the addresses it brought up:

```
Student view : http://localhost:5173/login
Admin port   : http://localhost:5173/manage
API          : http://localhost:8080/api/availability?date=…&building=Central%20Podium&from=14:00&to=15:50
```

Stop everything with `./scripts/stop-all.sh`.

---

## 2. What that command does

```
start-db.sh        initdb (first run) → cluster in db/pgdata → port 5433 → createdb emptyclassroom
start-backend.sh   mvn spring-boot:run → port 8080 → Flyway runs V1…V16 → tables + seed data
start-frontend.sh  npm install (first run) → Vite → port 5173
```

The backend waits for `http://localhost:8080/api/buildings` before the frontend line is printed,
so "up" means it answered, not that a timer expired.

---

## 3. Step by step

Use this if you want to watch each layer come up, or if the fast path failed somewhere.

### 3.1 Database

```bash
./scripts/start-db.sh
```

Expected:

```
[db] first run: initdb into …/db/pgdata
[db] ready: postgresql://<you>@localhost:5433/emptyclassroom
```

Check it:

```bash
psql -h 127.0.0.1 -p 5433 -U "$USER" -d emptyclassroom -c '\dt'
```

You should see `rooms`, `class_slots`, `app_user`, `room_updates`, `room_reviews`,
`review_photos`, `audit_log`, `flyway_schema_history`.

> Tables appear only after the backend has started once — Flyway creates them. If `\dt` shows
> nothing, that is expected at this point; go to 3.2.

### 3.2 Backend

```bash
./scripts/start-backend.sh          # keep this terminal open
```

Wait for `Started EcApplication`, then in another terminal:

```bash
curl -s http://localhost:8080/api/about
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/api/buildings
```

Expected: a JSON body, then `200`.

### 3.3 Desktop client

```bash
./scripts/start-frontend.sh
```

First run installs dependencies (about 30 s), then prints `Local: http://localhost:5173/`.

### 3.4 Phone client (optional)

```bash
cd mobile
npm install
npm run dev          # http://localhost:5174
```

It calls the same `/api` through a dev-server proxy, so **the backend does not change** and
there is no second API to keep in sync. Administrator pages are desktop-only.

### 3.5 Simulated school feed (optional)

A separate Spring Boot service with its **own** database (`mockschool`), used to test what
happens when the timetable comes from somewhere else.

```bash
./scripts/mock-school.sh            # http://127.0.0.1:9000  (own console at /)
./scripts/verify-mock.sh            # 10 end-to-end checks
```

The console lets you mark the feed stale, slow, or down, and edit the timetable by hand.

---

## 4. Accounts

| Account | Password | Role | Can do |
|---|---|---|---|
| `user1` / `user2` | `user123` | student | browse rooms, read and write own reviews |
| `admin1` / `teacher1` | `admin123` | administrator | lock or release room time, edit facilities, withdraw own changes |
| `super1` / `super2` | `super123` | super administrator | everything above, plus accounts and deleting anyone's review |

These are demo accounts created by migrations. They are not secrets; there is no real user data
in this repository.

---

## 5. Where the data lives

| | Location | In git? |
|---|---|---|
| Database cluster (71 MB) | `db/pgdata/` | no — `.gitignore` has `/db/` |
| Schema + seed | `backend/src/main/resources/db/migration/V1…V16` | yes |
| Simulated feed schema + seed | `mock-school-api/src/main/resources/db/migration/` | yes |
| Uploaded review photos | `backend/uploads/review-photos/` | no |

A data directory is binary and tied to one PostgreSQL build, so it is not portable. The
migrations are the reproducible part: any machine that runs the backend gets the same tables and
the same 10 rooms, 100 class slots, 6 users and 6 reviews.

---

## 6. Tests

```bash
bash scripts/test.sh
```

Expected tail:

```
Tests run: 31, Failures: 0, Errors: 0, Skipped: 0
```

---

## 7. Ports

| Port | What | Needed for |
|---|---|---|
| 5433 | PostgreSQL (project private) | everything |
| 8080 | Spring Boot API | both clients |
| 5173 | Desktop client (Vite) | the main app |
| 5174 | Phone client (Vite) | optional |
| 9000 | Simulated school feed | optional |

`stop-all.sh` stops 5433, 8080, 5173, 5174 and 9000.

---

## 8. Troubleshooting

**`Address already in use` on 5173, 5174, 8080 or 9000**

```bash
lsof -nP -iTCP:5173 -sTCP:LISTEN     # find the pid
kill <pid>                            # or ./scripts/stop-all.sh
```

If you have another Vite project running, 5174 can silently become 5175 and you will be looking at
the wrong app. This project pins its ports (`strictPort: true`), so it fails loudly instead.

**`FATAL: role "…" does not exist`**

The cluster was created by a different user. `start-db.sh` runs `initdb -U $USER`, so the role is
your shell user. Either connect as that user, or start from a clean cluster:

```bash
./scripts/stop-db.sh && rm -rf db/pgdata && ./scripts/start-db.sh
```

**`initdb: command not found`**

PostgreSQL 17 is not installed or not on `PATH`. On macOS: `brew install postgresql@17`. On
Debian/Ubuntu: `apt install postgresql-17`. Test with `initdb --version`.

**Backend starts but `\dt` is empty**

Flyway did not run. Check `logs/backend.log` for `Migrating schema`. In Spring Boot 4, Flyway
needs `spring-boot-starter-flyway` (not just `flyway-core`) — this project already uses the
starter, so an empty schema usually means the backend never finished starting.

**`Validate failed: Migration checksum mismatch`**

Someone edited an already-applied migration. Do not edit migrations that have run; add a new one.
For a scratch database: `./scripts/stop-db.sh && rm -rf db/pgdata && ./scripts/start-db.sh`.

**Time window rejected with 400**

The rules are whole blocks: `from` must be on the hour, `to` must end at `:50`
(`09:00–10:50`, `13:00–15:50`). A window that overlaps a class cannot be booked, and the school
timetable always wins over an administrator.

**Everything is free on a Thursday**

Fixed in `V16__thursday_slots.sql` — the first seed only covered Monday, Tuesday, Wednesday and
Friday. If you use an older database, run the migrations again or recreate the cluster.

---

## 9. Verified on a clean clone

Clone into a fresh directory and build all three pieces:

```bash
git clone https://github.com/HenryLiu520/hku-empty-classroom.git /tmp/fresh-clone
cd /tmp/fresh-clone/backend && mvn -o -q -DskipTests package    # → target/empty-classroom-0.1.0.jar (52 MB)
cd /tmp/fresh-clone/mobile  && npm ci && npm run build          # → 161 KB JS, 4 KB CSS
cd /tmp/fresh-clone/frontend && npm ci && npm run build         # → builds clean
```

All three succeeded on a clean clone, so nothing needed by the build is missing from the
repository.

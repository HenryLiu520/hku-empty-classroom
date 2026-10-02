# Empty Classroom Plan

**English** · [简体中文](README.zh-CN.md)

> A web app that answers one question about HKU teaching rooms: **which rooms are free between classes, right now?**
> Vue 3 + Spring Boot + PostgreSQL · COMP1110 Group 08 (HKU)

![Search page: which rooms are free from 14:00 to 15:50](docs/images/find-a-room.png)

---

## Why this exists

Between 09:50 and 10:10, the corridors of every HKU teaching building fill up with students looking for a seat. Each of them is asking the same question — *is there a free room nearby?* — and none of them can answer it without walking over to find out.

The information needed to answer it already exists. The University runs the weekly timetable, and it already pushes that data to the end of the chain: outside every classroom door there is a display showing that room's timetable for the day. What does not exist is a view that turns the timetable into *"which rooms are free in the next hour"* for a student who is standing in a corridor.

This project joins the two things that decide whether a room is usable — the **fixed weekly timetable** and **temporary changes posted by staff** (a room taken for an event, a room no longer needed) — and reports the free time that remains. It deliberately reports **rooms, not people**: it never guesses how full a room is, and it never asks a student to reserve anything.

## What it does

- **Search** — pick a day, a building and a time window (`14:00 → 15:50`) and see which rooms are free for all of it. Each result says whether the room is free now, free later, or in use.
- **Every room has a page** — its timetable for the day as a busy/free bar, its facilities (seats, sockets, seat type), and reviews from other students.
- **All rooms** — the full room list with sorting, independent of any time filter.
- **Reviews** — one star rating per person per room; administrators can remove any review, and removals are logged.
- **Facility edits** — seats / sockets / seat type are editable **only by administrators**; each save stamps a "last verified" time, and rooms that have never been verified say so on screen.
- **Staff changes** — an administrator can **add a use** of a room or **release** time, and every change carries an expiry date and an audit entry.
- **Sign-up** — anyone with an `hku.hk` address (`@connect.hku.hk`, `@hku.hk`, …) can create a student account with an email and a password. The prototype sends no verification email.

## Screenshots

| | |
|---|---|
| **Search a free window** | **A room's own page** |
| ![Search](docs/images/find-a-room.png) | ![Room page](docs/images/room-page.png) |
| **All rooms** | **Staff change screen** |
| ![All rooms](docs/images/all-rooms.png) | ![Admin](docs/images/admin-change.png) |
| **Facilities edit (administrators only)** | |
| ![Facilities](docs/images/facilities.png) | |

## How it works

The mental model is one sentence: **free time is the day minus the busy blocks.**

Draw one day as a single line from 08:00 to 22:00. Every class and every posted change is a block that gets coloured in. What is left uncoloured is the free time. Answering *"is this room free from 10:00 to 11:00?"* is then just checking whether that stretch stays inside an uncoloured part. The service does this by merging and subtracting blocks of time, and it keeps a ten-minute changeover margin on both sides of every block, because a room is not usable up to the minute a class ends.

Where the busy blocks come from is the part that matters most:

| | |
|---|---|
| **The University timetable is the authority** | A change posted by staff can only occupy time the timetable shows as free. The server rejects any change that overlaps a class (`400 TIMETABLE_PRIORITY`) — it does not silently ignore it. |
| **Students cannot affect occupancy** | Student accounts have no write path at all. The check happens on the server, not by hiding buttons: a student token gets `403` on the change endpoint. |
| **We ask, we do not copy** | The University's timetable is read from a read-only endpoint and polled, with a freshness check against a last-updated stamp. We keep no copy and never write to their system. In the prototype this source is sample data (see *Limitations*). |

### Rules the server enforces

| Rule | Where | How it was verified |
|---|---|---|
| Start on the hour, search end at `:50` | `AvailabilityService.requireSearchWindow` / `requireWholeHours` | `from=14:30` → `400`; `to=16:00` → `400` |
| Staff changes may not touch class time | `UpdateService.create` (overlap test) | `RELEASE`/`USE` over a class → `400 TIMETABLE_PRIORITY` |
| Only administrators can post changes | `AuthService.require(token, "admin")` | student token → `403 FORBIDDEN` |
| Only `hku.hk` addresses can register | `AuthService.isHkuEmail` | `@gmail.com` and `@connect.hku.hk.evil.com` → `400` |
| Every change is attributed and expirable | `room_updates`, `audit_log` | `GET /api/audit` |

## Architecture

![Architecture: one Vue app with two account roles, one service, one database, and the University's timetable as a read-only input](docs/images/architecture.png)

Three parts, and the split between them is the design:

1. **One Vue 3 single-page app, two account roles.** The student pages have no write control anywhere; the administrator pages are the same application with extra routes, and what they may do is decided by the server.
2. **One Spring Boot service** that computes availability, applies the rules above, and is the only part of the system that writes anything.
3. **One PostgreSQL database** holding the timetable, the posted changes, the audit log and the accounts.

The University's timetable is an **input, not a part we build**. Our service calls a read-only endpoint on a schedule and compares a last-updated stamp; if the data behind an answer is stale, the answer says so rather than pretending to be current.

## Quick start

**Requirements:** Java 21, Maven, Node 18+, PostgreSQL 17 (`brew install maven openjdk@21 postgresql@17 node` on macOS).

```bash
git clone https://github.com/HenryLiu520/hku-empty-classroom.git
cd hku-empty-classroom

./scripts/start-all.sh      # starts PostgreSQL (5433) → Spring Boot (8080) → Vite (5173)
# first run installs dependencies and applies the Flyway migrations: allow 3–8 minutes
```

Then open **http://localhost:5173/login** and sign in:

| Account | Role | What it shows |
|---|---|---|
| `user1` | `user` (student) | Search and room pages only — no write control exists in the UI |
| `admin1` | `admin` | The same app plus *Change room use time*, facility editing, and the audit trail |
| `teacher1` | `admin` | A second administrator, used to demonstrate managing another admin's changes |

Seeded passwords are in `backend/src/main/resources/db/migration/V2__seed.sql` (accounts renamed in `V5__account_names.sql`).

Stop everything with `./scripts/stop-all.sh`. Layers can be started individually with `scripts/start-db.sh`, `start-backend.sh`, `start-frontend.sh`.

Sanity check from the command line:

```bash
curl -s "http://localhost:8080/api/availability?date=$(date +%F)&building=CPD&from=14:00&to=15:50" | python3 -m json.tool | head -30
```

## API (prototype)

| Method | Path | Notes |
|---|---|---|
| `POST` | `/api/auth/register` | Sign up with an `hku.hk` email; returns a token |
| `POST` | `/api/auth/login` | Returns a token (kept in memory, 24 h) |
| `GET` | `/api/buildings` | Buildings |
| `GET` | `/api/rooms?building=CPD` | Room list, with facilities and review averages |
| `GET` | `/api/availability?date=&building=&from=&to=` | **Main query** — which rooms are free for the whole window |
| `GET` | `/api/rooms/{code}/timeline?date=` | Busy / free / buffer segments for one room, one day |
| `POST` | `/api/updates` | Post a change (`USE` / `RELEASE`) — administrators only |
| `GET` | `/api/updates/mine` · `GET`/`DELETE` `/api/updates[/{id}]` | List, inspect and cancel changes (cancel keeps the record, marks it expired) |
| `PATCH` | `/api/rooms/{code}` | Edit facilities — administrators only |
| `GET` | `/api/rooms/{code}/reviews` · `POST` · `DELETE /api/reviews/{id}` | Reviews (one per person per room) |
| `GET` | `/api/audit` | Audit trail — administrators only |

## Tests

```bash
./scripts/test.sh        # needs the database running; the script starts it if needed
```

**20 automated tests, all passing** (9 interval-maths unit tests, 9 availability/rule tests, 2 sign-up rule tests). The boundary cases the report leans on:

| Case | Test | |
|---|---|---|
| A normal gap between two classes is usable | `normalGapIsUsable` | ✓ |
| Two classes back to back give no window | `backToBackGivesNoWindow` | ✓ |
| A gap shorter than the changeover margin is not offered | `gapShorterThanBufferIsNotOffered` | ✓ |
| A release can only cancel a staff-added block, never a class | `releaseOnlyCancelsAnAdminAddition` | ✓ |
| A release can never punch a hole in the timetable | `releaseCannotOverrideTheTimetable` | ✓ |
| A change that overlaps a class is rejected outright | `adminCannotChangeClassTime` | ✓ |
| During a class the room reports *in use* | `duringClassIsInUse` | ✓ |
| Non-hour times are rejected | `offHourTimesAreRejected` | ✓ |
| Only `hku.hk` addresses may register | `RegistrationRulesTest` (2 cases) | ✓ |

Tests are `@Transactional` and roll back, so running them never pollutes the demo data.

## Performance (measured 2026-10-02)

| Metric | Value |
|---|---|
| Throughput of the search endpoint | **~8,500 requests/s** (stable from 50 to 1000 concurrent) |
| Latency | 5.9 ms @ 50 concurrent · 121 ms @ 1000 concurrent |
| SQL statements per request | **3** (rooms + that day's classes + that day's changes) |
| Heaviest run | 1000 concurrent × 10 s → 84,713 requests, all `200`, 0 errors |

The first version issued 31 statements per request (one class query and one change query per room — an N+1), which made the database the bottleneck at ~1,100 requests/s. Batching the two lookups and grouping in memory cut it to 3 statements and raised throughput 7.7×. Redis was deliberately not used: one instance, read-mostly, a few KB of data — Redis matters for a multi-instance deployment, not for this load.

## Project layout

```
empty-classroom/
├── backend/                      Spring Boot 4.1.1 · Java 21 · Maven
│   └── src/main/java/hku/ec/
│       ├── domain/               Room · ClassSlot · RoomUpdate · AuditEntry · AppUser
│       ├── repo/                 Spring Data JPA repositories
│       ├── service/
│       │   ├── AvailabilityService.java   ★ the free-interval computation
│       │   ├── UpdateService.java         staff changes, expiry, timetable-priority check
│       │   └── AuthService.java           roles, sign-up rule, prototype tokens
│       └── web/                  controllers and DTOs
│   └── src/main/resources/
│       ├── application.yml       changeover buffer, opening hours, datasource
│       └── db/migration/         Flyway V1…V9 (schema, seed, reviews, facilities, sign-up)
├── frontend/                     Vue 3 · Vite · Element Plus · Pinia
│   └── src/views/                Login.vue · FindRoom.vue · AllRooms.vue (with the room panel) · AdminUpdates.vue
├── docs/images/                  screenshots used above
├── db/pgdata/                    project-local PostgreSQL data directory (safe to delete and rebuild)
├── logs/                         backend.log · frontend.log · pg.log
└── scripts/                      start-all.sh · stop-all.sh · test.sh · per-layer scripts
```

## Limitations, and what we do not claim

| Limitation | Detail |
|---|---|
| **"Free" does not mean "empty"** | The system answers *"is this room taken by a class or a posted change?"*, not *"how many people are in it?"*. Rooms are shared; the UI says so explicitly. |
| The timetable is sample data | `V2__seed.sql` is a plausible mock, not the real timetable, and the University's endpoint is not connected yet. Every figure in the report must come from real data before it is claimed. |
| Prototype-grade auth | Tokens live in memory (a restart invalidates them), passwords use `SHA-256(salt:password)` rather than bcrypt, and there is no HTTPS. This is not a production authentication design. |
| No verification email | Sign-up checks the address suffix only; anyone can type any `hku.hk`-shaped address. |
| Reviews are not moderated | "One per person per room, deletable by the author, removable by an administrator" — there is no report or filtering queue. |
| Facilities are sample values | Pre-filled placeholders per room type; the screen says `sample data, not verified yet` until an administrator saves. Do not present them as surveyed. |
| Single instance | No clustering; the expiry sweep is a single `@Scheduled` job. |
| CORS configured per method | Adding an HTTP method means adding it to `WebConfig.allowedMethods` as well — a missing `PATCH` once produced `403 Invalid CORS request` from the browser while `curl` (no `Origin` header) still passed. |

## Licence and credits

This repository is coursework and is not distributed, so it carries no licence file of its own. All third-party components are used under permissive licences — MIT for the front-end stack (Vue 3, Vite, Element Plus, Pinia, axios), Apache-2.0 for Spring Boot and Flyway, the PostgreSQL licence for the database, BSD-2-Clause for the JDBC driver — and each is credited in [THIRD-PARTY.md](THIRD-PARTY.md). The interface follows the sidebar-dashboard idiom popularised by youlai's [vue3-element-admin](https://github.com/youlaitech/vue3-element-admin) (MIT); **no code from that template is copied into this project**.

## Context

COMP1110 Group Project, Group 08 — The University of Hong Kong, 2026. Under the course's Group Project Guidelines, code and prototypes are optional and are not graded: this prototype exists to show that the design in our Project Plan actually runs.

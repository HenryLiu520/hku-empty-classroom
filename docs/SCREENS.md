# Screens

Captured from the running project on **22 October 2026** and grouped by which piece of the system
they come from. Every image is a real screenshot of the app, not a mock-up.

---

## 1. Desktop client

1440 × 900 viewport, captured at 2× for sharp text.

| | |
|---|---|
| **Sign in** — one login for all three roles | **Find a free room** — building, date, one window |
| ![Sign in](screens/desktop/01-sign-in.jpg) | ![Find a free room](screens/desktop/02-find-a-room.jpg) |
| **Room and reviews** — timetable strip, reviews, one with a photo | **Accounts** — super administrators only |
| ![Room and reviews](screens/desktop/03-room-and-reviews.jpg) | ![Accounts](screens/desktop/04-accounts.jpg) |

What to look for: the timetable strip is drawn from the school timetable, not from a bookable
calendar; the review at the bottom carries a photo uploaded through the API; the account screen is
reachable only by a super administrator.

---

## 2. Phone client

iPhone 13 viewport (390 × 844), the same backend as the desktop.

| | | |
|---|---|---|
| **Find** — three rooms free for the window | **Room** — timetable strip and reviews | **Photo, tapped** — full screen |
| ![Find](screens/mobile/01-find.jpg) | ![Room](screens/mobile/02-room.jpg) | ![Photo](screens/mobile/03-photo-fullscreen.jpg) |

| | |
|---|---|
| **Me** — account, role, language | |
| ![Me](screens/mobile/04-me.jpg) | |

---

## 3. Simulated school feed

A separate service on port 9000 with its own database (`mockschool`). It exists so the project can
be tested against something that behaves like a school system: data that can go stale, responses
that can slow down, an upstream that can fail.

![Console](screens/mock/01-console.jpg)

The console shows the feed's `revision` and `lastUpdated`, switches that make the data stale, slow
or unavailable, a timetable grid for visual checking, and the room rows that can be edited in
place (a write goes straight to the database and bumps `revision`).

---

## 4. Diagrams

| | |
|---|---|
| **Two systems, one contract** — how the app calls the feed | [two-systems-architecture-en.png](../mock-school-api/design/figures/two-systems-architecture-en.png) |
| **Overall architecture** | [architecture.png](images/architecture.png) |

# 介面截圖

**2026 年 10 月 22 日**從跑著的專案裡截的，按系統裡的不同部分分類。每張都是應用程式的真實截圖，不是示意圖。

---

## 1. 桌面端

1440 × 900 視窗，2 倍像素，字更清楚。

| | |
|---|---|
| **登入** —— 三個角色共用一個登入 | **找空教室** —— 樓棟、日期、一個時間窗 |
| ![登入](screens/desktop/01-sign-in.jpg) | ![找空教室](screens/desktop/02-find-a-room.jpg) |
| **教室與評價** —— 課表條、評價，其中一條帶照片 | **帳號管理** —— 只有超級管理員能進 |
| ![教室與評價](screens/desktop/03-room-and-reviews.jpg) | ![帳號](screens/desktop/04-accounts.jpg) |

看圖時注意三點：課表條畫的是**學校課表**，不是可預約的行事曆；最下面那條評價帶的照片是**透過介面上傳**的；帳號頁只有超級管理員能打開。

---

## 2. 手機端

iPhone 13 視窗（390 × 844），**同一個後端**。

| | | |
|---|---|---|
| **找教室** —— 該時段 3 間空閒 | **教室頁** —— 課表條 + 評價 | **點開照片** —— 全螢幕 |
| ![找教室](screens/mobile/01-find.jpg) | ![教室](screens/mobile/02-room.jpg) | ![照片](screens/mobile/03-photo-fullscreen.jpg) |

| | |
|---|---|
| **我的** —— 帳號、角色、語言 | |
| ![我的](screens/mobile/04-me.jpg) | |

---

## 3. 模擬學校系統

一個獨立服務，連接埠 9000，**自己的資料庫**（`mockschool`）。它存在的意義是：讓專案能對著「像學校系統的東西」做測試 —— 資料會變舊、回應會變慢、上游會掛。

![控制台](screens/mock/01-console.jpg)

控制台上能看到這份資料的 `revision` 和 `lastUpdated`；有把資料變舊、變慢、變不可用的開關；有課表網格做視覺化核對；最下面是教室列，可以直接改（改完寫進資料庫並讓 `revision` 加一）。

---

## 4. 架構圖

| | |
|---|---|
| **兩個系統，一份契約** —— 應用程式怎麼呼叫這份資料 | [two-systems-architecture-en.png](../mock-school-api/design/figures/two-systems-architecture-en.png) |
| **整體架構** | [architecture.png](diagrams/architecture.png) |

# Project Beast — Android App Integration Guide

Everything the Android app needs to connect to the Project Beast backend and database.

---

## 1. Architecture Overview

```
┌─────────────┐       HTTPS        ┌──────────────────────┐      Prisma      ┌─────────────┐
│ Android App │ ──────────────────► │ Vercel (Next.js API) │ ───────────────► │  Neon DB     │
│             │                    │ leetcodee-sigma      │                  │ PostgreSQL   │
└─────────────┘                    └──────────────────────┘                  └─────────────┘
       │                                    │
       │ Clerk SDK                          │ Clerk Secret Key
       │ (Publishable Key)                  │ DATABASE_URL
       ▼                                    ▼
   Login/Auth                        Handles everything
```

> [!IMPORTANT]
> The Android app **never** connects to the database directly. It calls the API hosted on Vercel, which talks to the database securely.

---

## 2. What the Android App Needs

### Only 2 config values:

```
API_BASE_URL = "https://leetcodee-sigma.vercel.app/api"
CLERK_PUBLISHABLE_KEY = "pk_test_c3BlY2lhbC1za3Vuay05Ni5jbGVyay5hY2NvdW50cy5kZXYk"
```

### 1 asset file (for offline sheet data):

```
striver_a2z.json  →  Copy into Android app's  app/src/main/assets/
```

> [!CAUTION]
> **NEVER** put these in the Android app:
> - `DATABASE_URL` — anyone can decompile your APK and steal this
> - `CLERK_SECRET_KEY` — same risk, server-only

---

## 3. Authentication Flow

All protected API endpoints (marked with 🔒) require a Clerk session token.

**Step 1:** User logs in via Clerk Android SDK using the Publishable Key

**Step 2:** Clerk returns a session token (JWT)

**Step 3:** Attach the token to every API request:

```
Headers:
  Authorization: Bearer <CLERK_SESSION_TOKEN>
  Content-Type: application/json
```

**Public endpoints** (no auth needed): Leaderboard, Daily Challenge, LeetCode Sync, User Status

---

## 4. Sheet Questions — Offline JSON

The Striver A2Z sheet questions are bundled as a **local JSON file** in the app. No API call needed to load the question list.

**File:** `striver_a2z.json` (250+ problems, ~66KB)

**Format:**
```json
[
  {
    "title": "Reverse Integer",
    "slug": "reverse-integer",
    "url": "https://leetcode.com/problems/reverse-integer/",
    "platform": "LeetCode",
    "topic": "Learn the basics",
    "step": ""
  },
  {
    "title": "Two Sum",
    "slug": "two-sum",
    "url": "https://leetcode.com/problems/two-sum/",
    "platform": "LeetCode",
    "topic": "Solve Problems on Arrays [Easy -> Medium -> Hard]",
    "step": ""
  }
]
```

**All 17 topics (in order):**

| # | Topic |
|---|-------|
| 1 | Learn the basics |
| 2 | Solve Problems on Arrays [Easy -> Medium -> Hard] |
| 3 | Binary Search [1D, 2D Arrays, Search Space] |
| 4 | Strings [Basic and Medium] |
| 5 | Learn LinkedList [Single LL, Double LL, Medium, Hard Problems] |
| 6 | Recursion [PatternWise] |
| 7 | Bit Manipulation [Concepts & Problems] |
| 8 | Stack and Queues [Learning, Pre-In-Post-fix, Monotonic Stack, Implementation] |
| 9 | Sliding Window & Two Pointer Combined Problems |
| 10 | Heaps [Learning, Medium, Hard Problems] |
| 11 | Greedy Algorithms [Easy, Medium/Hard] |
| 12 | Binary Trees [Traversals, Medium and Hard Problems] |
| 13 | Binary Search Trees [Concept and Problems] |
| 14 | Graphs [Concepts & Problems] |
| 15 | Dynamic Programming [Patterns and Problems] |
| 16 | Tries |
| 17 | Strings [Hard Problems] |

**Reading in Java:**
```java
InputStream is = getAssets().open("striver_a2z.json");
String json = new String(is.readAllBytes());
List<Problem> problems = new Gson().fromJson(json, new TypeToken<List<Problem>>(){}.getType());
```

---

## 5. Complete API Endpoint Reference

**Base URL:** `https://leetcodee-sigma.vercel.app/api`

---

### 5.1 Sheet Progress (Multiplayer — Who solved what) 🔒

This is how the app shows checkmarks next to questions that users have completed. **Same data syncs between website and Android app.**

#### `GET /api/sheets/striver/progress`
Get all users' progress on the Striver sheet.

**Response:**
```json
{
  "sheetId": "striver",
  "users": [
    {
      "userId": "clxyz...",
      "username": "yash_pro",
      "avatarUrl": "https://...",
      "clerkId": "user_abc...",
      "completed": ["reverse-integer", "two-sum", "binary-search"],
      "inProgress": ["merge-intervals"]
    }
  ]
}
```

#### `POST /api/sheets/striver/progress`
Mark a problem as completed or uncompleted.

**Request:**
```json
{ "problemId": "two-sum", "completed": true }
```

**Response:**
```json
{
  "success": true,
  "progress": {
    "problemId": "two-sum",
    "status": "COMPLETED",
    "completedAt": "2026-04-06T10:00:00Z"
  }
}
```

---

### 5.2 User & LeetCode Account

#### `GET /api/user/leetcode` 🔒
Get linked LeetCode username.

**Response:** `{ "leetcodeUsername": "yashsmars" }`

#### `POST /api/user/leetcode` 🔒
Link or update LeetCode username.

**Request:** `{ "leetcodeUsername": "yashsmars" }`
**Response:** `{ "success": true, "leetcodeUsername": "yashsmars" }`

#### `DELETE /api/user/leetcode` 🔒
Unlink LeetCode account.

**Response:** `{ "success": true }`

#### `GET /api/user/status/today?username=yashsmars`
Check if user solved a problem today. **Public**.

**Response:**
```json
{
  "hasSolved": true,
  "timestamp": "2026-04-06T10:00:00Z",
  "problemsToday": 2,
  "currentStreak": 5
}
```

---

### 5.3 LeetCode Data (Proxy APIs — Public, no auth)

#### `POST /api/leetcode/sync`
Fetch user profile, stats, submissions, calendar from LeetCode.

**Request:** `{ "username": "yashsmars" }`

**Response:**
```json
{
  "matchedUser": {
    "username": "yashsmars",
    "profile": {
      "realName": "Yash",
      "userAvatar": "https://...",
      "ranking": 123456
    },
    "submitStatsGlobal": {
      "acSubmissionNum": [
        { "difficulty": "All", "count": 35 },
        { "difficulty": "Easy", "count": 13 },
        { "difficulty": "Medium", "count": 22 },
        { "difficulty": "Hard", "count": 0 }
      ]
    },
    "userCalendar": {
      "streak": 1,
      "totalActiveDays": 40,
      "submissionCalendar": "{\"1704067200\":1, ...}"
    }
  },
  "recentSubmissionList": [
    {
      "title": "Two Sum",
      "titleSlug": "two-sum",
      "timestamp": "1712345678",
      "statusDisplay": "Accepted",
      "lang": "java"
    }
  ]
}
```

#### `GET /api/leetcode/daily`
Get today's LeetCode daily challenge.

**Response:**
```json
{
  "date": "2026-04-06",
  "title": "Largest Magic Square",
  "titleSlug": "largest-magic-square",
  "difficulty": "Medium",
  "questionId": "1895"
}
```

#### `POST /api/leetcode/check-solved`
Check if user solved a specific problem (used during Dojo battles).

**Request:**
```json
{
  "username": "yashsmars",
  "problemSlug": "two-sum",
  "afterTimestamp": 1712345678000
}
```

**Response:** `{ "solved": true, "timestamp": 1712345999000, "language": "java" }`

---

### 5.4 Coins & Economy

#### `GET /api/coins` 🔒
Get coin balance.

**Response:** `{ "coins": 100 }`

#### `POST /api/coins` 🔒
Add or deduct coins.

**Request:**
```json
{
  "amount": 50,
  "type": "DAILY_REWARD",
  "description": "Solved daily challenge"
}
```

**Transaction types:** `BATTLE_WIN`, `BATTLE_ENTRY`, `DAILY_REWARD`, `STREAK_BONUS`, `SHOP_PURCHASE`, `LEAGUE_PROMOTION`, `REFERRAL`

**Response:** `{ "success": true, "newBalance": 150 }`

---

### 5.5 Leaderboard (Public — no auth)

#### `GET /api/leaderboard?tier=GOLD&limit=50`

**Query params:** `tier` (optional), `limit` (default 100)

**Tiers:** `BRONZE`, `SILVER`, `GOLD`, `PLATINUM`, `DIAMOND`, `BEAST`

**Response:**
```json
{
  "leaderboard": [
    {
      "rank": 1,
      "id": "clxyz...",
      "username": "yashsmars_034",
      "avatarUrl": "https://...",
      "xp": 875,
      "level": 3,
      "leagueTier": "BRONZE",
      "currentStreak": 4,
      "totalSolved": 47,
      "coins": 100
    }
  ],
  "weekStart": "2026-03-31T00:00:00.000Z",
  "totalUsers": 3
}
```

---

### 5.6 Shop

#### `GET /api/shop` (Public)
Get shop items.

**Response:**
```json
{
  "items": [
    {
      "id": "...",
      "name": "Neon Profile Frame",
      "description": "A cool frame",
      "type": "PROFILE_FRAME",
      "price": 200,
      "isLimited": false,
      "stock": null
    }
  ]
}
```

**Item types:** `PROFILE_FRAME`, `EDITOR_THEME`, `STREAK_FREEZE`, `XP_BOOST`, `TITLE`

#### `POST /api/shop` 🔒
Purchase an item.

**Request:** `{ "itemId": "clxyz123..." }`
**Response:** `{ "success": true, "message": "Purchased Neon Profile Frame!", "newBalance": 100 }`

---

### 5.7 Friends & Social

#### `GET /api/friends` 🔒
Get friend list.

**Response:**
```json
{
  "friends": [
    {
      "friendshipId": "...",
      "isRival": false,
      "username": "coder123",
      "avatarUrl": "https://...",
      "leetcodeUsername": "coder123",
      "xp": 450,
      "level": 2,
      "leagueTier": "BRONZE",
      "currentStreak": 3
    }
  ]
}
```

#### `POST /api/friends` 🔒
Send friend request.

**Request:** `{ "username": "coder123" }`
**Response:** `{ "success": true, "friendship": { "id": "...", "status": "PENDING" } }`

---

## 6. Socket.IO Events (Dojo Battles)

For real-time battle functionality, connect via Socket.IO.

**Connection URL:** `http://10.0.2.2:3001` (emulator) or `http://192.168.x.x:3001` (device)

> [!NOTE]
> The Dojo server runs separately from the Next.js app. It needs to be running on your PC or deployed to Render.

**Events to emit:**

| Event | Payload |
|-------|---------|
| `room:create` | `{ userId, username, avatarUrl, difficulty, duration, isHardcore, entryFee, problemSlug?, leetCodeRoomUrl? }` |
| `room:join` | `{ roomCode, userId, username, avatarUrl }` |

**Events to listen:**

| Event | Description |
|-------|-------------|
| `room:created` | Room created (returns room object with `code`) |
| `room:joined` | Player joined |
| `room:update` | Room state changed |
| `battle:start` | Battle started |
| `battle:end` | Battle finished |

---

## 7. Database Schema (Quick Reference)

| Model | Key Fields |
|-------|-----------|
| **User** | `clerkId`, `username`, `leetcodeUsername`, `xp`, `level`, `coins`, `currentStreak`, `totalSolved`, `leagueTier` |
| **Submission** | `problemSlug`, `difficulty`, `status`, `language`, `runtime` |
| **Room** | `code` (6-char), `status`, `problemSlug`, `difficulty`, `duration`, `isHardcore` |
| **Friend** | `initiatorId`, `receiverId`, `status`, `isRival` |
| **ShopItem** | `name`, `type`, `price`, `isLimited`, `stock` |
| **SheetProgress** | `userId`, `sheetId`, `problemId`, `status` |
| **DailyProgress** | `userId`, `date`, `problemsSolved`, `xpEarned` |

**League Tiers:** `BRONZE` → `SILVER` → `GOLD` → `PLATINUM` → `DIAMOND` → `BEAST`

**Difficulty:** `EASY`, `MEDIUM`, `HARD`

---

## 8. Quick Start Checklist

- [ ] Set `API_BASE_URL` to `https://leetcodee-sigma.vercel.app/api`
- [ ] Set `CLERK_PUBLISHABLE_KEY` to `pk_test_c3BlY2lhbC1za3Vuay05Ni5jbGVyay5hY2NvdW50cy5kZXYk`
- [ ] Copy `striver_a2z.json` into `app/src/main/assets/`
- [ ] Implement Clerk login → get session token
- [ ] Add `Authorization: Bearer <token>` header to all 🔒 API calls
- [ ] Test `GET /api/leaderboard` first (no auth needed — easiest to verify)
- [ ] Load sheet questions from local JSON, progress from `/api/sheets/striver/progress`

---

## 9. Test URLs (Open in browser to verify)

✅ Working right now:
- https://leetcodee-sigma.vercel.app/api/leaderboard
- https://leetcodee-sigma.vercel.app/api/leetcode/daily
- https://leetcodee-sigma.vercel.app/api/shop
  https://leetcodee-sigma.vercel.app/api/sheets/striver/progress

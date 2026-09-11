# Physical test — Realme GT 6T

Worker never fills the Result column. Laraib or brother does.

Install the **rolling preview** APK from the private GitHub Release. This is sideload, not Play.

## Install

1. Download `khidki-1.1.0-debug-b*.apk` from GitHub Releases → **Khidki preview (rolling)** (`preview` tag).
2. Do **not** use stale `v1.0.0-preview` milestone unless intentionally pinned.
3. See `docs/RELEASE.md` for checksum verification.

## Before any SMS

1. Open Khidki → **Status** tab → sideload pre-flight if shown:
   - **Open App Info** → **⋮** → **Allow restricted settings** → return → **Grant SMS Permissions**.
2. Realme GT 6T: **Auto-start ON**, **Allow background activity**, battery **Unrestricted**.
3. Khidki is **not** the default SMS app (and must not become one).
4. Keep **Status → Live diagnostic stream** visible during trials.

## Critical concepts (read before testing)

### Master ON vs `req` (common confusion)

| Control | What it does |
|---|---|
| **Master OFF** | Engine stopped. All SMS ignored. No `[IN]` lines. |
| **Master ON** | Engine listening. SMS processed. **Does not auto-forward OTPs.** |
| **`req <password>`** | Arms a **2-minute forwarding window** for that rule. Required before OTP forward. |

**Master ON does not bypass `req`.** Both are required for forwarding.

### SMS vs RCS

| Message type | Example | Khidki sees it? |
|---|---|---|
| **SMS** | Blinkit OTP (`CP-blnkit-S`), bank OTP | **Yes** — `[IN]` in diagnostic stream |
| **RCS** | Google Messages chat with Chotu, "RCS chat" header | **No** — invisible to Khidki |

- **`req` from brother must be SMS**, not RCS, unless Phase 7 manual arm is implemented.
- **OTP sources** (Blinkit, banks) are SMS — they work once a window is armed.

### Diagnostic stream vs History

| Screen | Shows | When empty |
|---|---|---|
| **Status → diagnostic stream** | Live in-memory log (`[IN]`, `[CMD]`, `[CANDIDATE]`) | No SMS received, or master OFF |
| **History → audit trail** | Persisted engine events (session armed, forwarded, auth failure) | Nothing reached engine audit layer |

`CANDIDATE: NoActiveSession` appears in **diagnostic stream only** — History stays at 0. This is expected when OTP arrives without an armed window.

## Recommended test flow (2026-09-11)

Use rule **"Chotu Test"** (requester = brother's number, sender `.*`, content OTP regex).

### Path A — SMS `req` (if brother can send SMS)

1. Master **ON**.
2. Brother sends **SMS** (not RCS): `req <8-digit-password>` from configured requester number.
3. Status: `[CMD] SessionCreated` + active window card with countdown.
4. Within 2 min: trigger Blinkit login (or send test OTP SMS).
5. Status: `[CANDIDATE] Forwarded`. Brother receives forwarded OTP SMS.
6. Fill P3 row below.

### Path B — Manual arm (Phase 7, not yet built)

1. Master **ON**.
2. Laraib taps **"Open window for Chotu Test"** in Status (future feature).
3. Trigger Blinkit login within 2 min.
4. Verify forward to brother.

### Path C — Diagnostic only (current blocker state)

If brother cannot send SMS `req`:

- Blinkit OTP will show: `[IN]` → `CANDIDATE: NoActiveSession` ✓ (SMS works, window not armed)
- Forward will **not** happen until Path A or B completes.

## Trials (original matrix)

Use brother's phone as peer. Never a live bank OTP in synthetic tests; Blinkit re-login is acceptable for integration test.

| ID | What to send / do | Phone state | Result (pass / fail / time) | Notes |
|---|---|---|---|---|
| P0 | Restricted settings + SMS grant | — | **PASS** | 2026-09-11 |
| P0b | Phase 6 UI usable (Configs, Status, Settings) | — | **PASS** | 2026-09-11 |
| P0c | SMS ingress (Blinkit OTP) | Master ON | **PASS** | `[IN]` + `NoActiveSession` at 15:22 |
| P0d | RCS `req` from Chotu | Master ON | **FAIL (expected)** | RCS invisible; not a code bug |
| P1 | Peer → GT 6T: `KHIDKI-M0-PLAIN 001` | Khidki not in front | | Must appear in diagnostic stream |
| P2 | Peer → GT 6T: `KHIDKI-M0-PLAIN 002` | Screen off | | |
| P3 | Arm window → Blinkit/bank OTP | UI closed | | **Product gate.** Requires armed window first |
| P4 | Peer → GT 6T: `<#> 482193 XYZaBcdEfGh` | UI closed | | Retriever-shaped |
| P5 | Peer → GT 6T: `KHIDKI-M0-UNICODE नमस्ते` | UI closed | | |
| P6 | Khidki Send to peer: `KHIDKI-M0-SEND 001` | UI open | | |
| P7 | Repeat P6 | Screen off | | |
| P8 | Revoke SMS permission, then P1 | UI closed | | Must not crash |
| P9 | Airplane mode, then P6 | Any | | Send fails. No retry |
| P10 | Reboot, unlock, then P3 | After first unlock | | |

A miss on P1/P2 is a platform failure. P3 requires an armed window — do not test OTP forward without `req` or manual arm.

When finished, paste this table into an issue or reply `GO` / `STOP` with the filled table.

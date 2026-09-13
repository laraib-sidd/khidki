# Physical test — Realme GT 6T

Worker never fills the Result column. The device owner does.

Install the **Latest** APK from GitHub Releases (sideload) or F-Droid once listed.

## Install

1. Download `khidki-1.1.0-debug-b*.apk` from GitHub Releases → **Latest**.
2. Record the release tag (e.g. `v1.1.0-b17`) in trial notes for reproducibility.
3. See `docs/RELEASE.md` for checksum verification.

## Before any SMS

1. Open Khidki → **Status** tab → sideload pre-flight if shown:
   - **Open App Info** → **⋮** → **Allow restricted settings** → return → **Grant SMS Permissions**.
2. Realme GT 6T: **Auto-start ON**, **Allow background activity**, battery **Unrestricted**.
3. Khidki is **not** the default SMS app (and must not become one).
4. Keep **Status → Live diagnostic stream** visible during trials.

## Critical concepts (read before testing)

### Start forwarding vs incoming SMS

| Control | What it does |
|---|---|
| **No active window** | Engine idle. OTP SMS may appear in diagnostics but are not forwarded. |
| **Start forwarding (Home)** | Arms a window (15m–2h or Until I stop) for enabled people on Rules. |
| **Stop (Home or notification shade)** | Ends the window immediately. |

You must **Start forwarding** before OTPs forward. Rules alone does not arm a window.

### Until I stop and reboot

**Until I stop** keeps forwarding until you tap Stop or the phone reboots. After a reboot, start forwarding again (boot clears the session by design).

### SMS vs RCS

| Message type | Example | Khidki sees it? |
|---|---|---|
| **SMS** | Blinkit OTP (`CP-blnkit-S`), bank OTP | **Yes** — `[IN]` in diagnostic stream |
| **RCS** | Google Messages RCS chat header | **No** — invisible to Khidki |

OTP sources (Blinkit, banks) are SMS — they work once a window is armed.

### Diagnostic stream vs History

| Screen | Shows | When empty |
|---|---|---|
| **Home → recent activity** | Live in-memory log (`[IN]`, `[CANDIDATE]`) | No SMS received, or no active window |
| **History → audit trail** | Persisted engine events (armed, forwarded, stopped) | Nothing reached engine audit layer |

`CANDIDATE: NoActiveSession` in diagnostics is expected when OTP arrives without an armed window.


## Recommended test flow (2026-09-11)

Use a test person whose number is the **peer device**.

### Path A — Home Start forwarding (v1.5)

1. Rules: at least one person with filters enabled.
2. Home: pick duration (or **Until I stop**) → **Start forwarding**.
3. Home: active card with countdown or Until I stop label.
4. Within 2 min: trigger Blinkit login (or send test OTP SMS).
5. Status: `[CANDIDATE] Forwarded`. Peer receives forwarded OTP SMS.
6. Fill P3 row below.

**Without Start forwarding:** Blinkit OTP will show `[IN]` → `CANDIDATE: NoActiveSession` (SMS ingress works; window not armed). Forward will not happen until you tap **Start forwarding** on Home.

## Trials (original matrix)

Use a second phone as peer. Never a live bank OTP in synthetic tests; Blinkit re-login is acceptable for integration test.

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

A miss on P1/P2 is a platform failure. P3 requires an armed window — tap **Start forwarding** on Home before testing OTP forward.

When finished, paste this table into an issue or reply `GO` / `STOP` with the filled table.

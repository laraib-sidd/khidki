# Khidki architecture

On-device Android app. You start a forwarding window from Home; matching SMS is forwarded to the people and categories you configured on Rules. No server. No `INTERNET` permission.

## Layers

| Layer | Package | Role |
|---|---|---|
| UI | `dev.laraib.khidki.ui` | Compose: Home (Status), Rules, History, Settings |
| Platform | `dev.laraib.khidki.platform` | SMS receiver, SmsManager transport, boot receiver, notifications |
| Domain | `dev.laraib.khidki.domain` | RE2/J filters, session engine, budget (JVM-testable) |
| Data | `dev.laraib.khidki.data` | Room, preferences, Keystore (legacy credential tables unused in v1.5 UI) |
| Bridge | `dev.laraib.khidki.data.adapter` | Sync adapters from suspend Room to domain ports |

## Flow

1. `SmsReceivedReceiver` receives `SMS_RECEIVED`.
2. `SmsInboundProcessor` treats every inbound SMS as a forward **candidate** (no command parsing).
3. `ForwardingEngine` matches against active session destinations and policies, sends via `AndroidSmsTransport`.
4. Metadata-only events persist in Room `history`.

## Timed forwarding (v1.5)

Owner arms a window from **Home → Start forwarding**:

- **People:** up to five destinations on Rules; each has their own filter toggles.
- **Duration:** 15m–2h presets or **Until I stop** (survives until Stop or phone reboot).
- **Origin:** `AuthorizationSession.origin = TIMED`.
- **Multi-forward:** TIMED sessions return to `ARMED` after each successful forward; `forwardCount` increments; notification count refreshes on each `CANDIDATE_FORWARDED`.
- **Budget:** each outbound SMS is capped at 3 parts; daily rolling budget applies per send, not multiplied by fan-out count.
- **Termination:** Stop (Home or notification shade), timed expiry, or reboot (`bootId` mismatch) — audit types `TIMED_CANCELLED` / `TIMED_EXPIRED`.

Room `sessions` stores `origin`, `forwardCount`, and destination snapshots.

## Constraints

- No server, no INTERNET permission.
- One global armed session at a time.
- SMS only — not RCS.
- Legacy `credentials` / `configurations` tables remain for Room v4; v1.5 UI does not use SMS `req` commands.

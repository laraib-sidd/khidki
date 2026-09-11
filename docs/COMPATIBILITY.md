# Compatibility

Last verified: 2026-09-11 (CI + partial physical GT 6T).

## Toolchain

| Item | Value |
|---|---|
| Kotlin | 2.1.20 |
| AGP | 8.9.1 |
| minSdk | 31 |
| compileSdk / targetSdk | 36 |
| JVM | 17 |

## CI

- OS: `ubuntu-latest` (GitHub Actions)
- Workflows: `ci` (PR + main), `release` (after green CI on main, or `v*` tags)
- Command: `./gradlew :app:testDebugUnitTest :app:assembleDebug`
- Unit tests: **62 passed** (2026-09-11)
- APK artifact: `khidki-<version>-debug-b<build>-<sha>.apk` via `v<version>-b<N>` GitHub Release

## Message transport matrix

| Transport | Khidki support | Use case | Notes |
|---|---|---|---|
| **SMS** (`SMS_RECEIVED`) | **Supported** | Bank OTP, Blinkit OTP, `req` command | Only channel Khidki can read |
| **RCS** (Google Messages Chat) | **Not supported** | Personal chats between phones | No public API; excluded by `docs/DECISIONS.md` |
| **MMS** | Not supported | — | Out of scope v1 |

## Physical device (Realme GT 6T, Android 16)

| Capability | Status | Date | Evidence |
|---|---|---|---|
| Sideload + restricted SMS permission | **Pass** | 2026-09-11 | Owner confirmed setup flow |
| SMS receive (transactional) | **Pass** | 2026-09-11 | Blinkit OTP → `[IN]` in diagnostic stream |
| SMS `req` from requester | **Blocked** | 2026-09-11 | Brother sends RCS only; Khidki never sees it |
| OTP forward to requester | **Not tested** | — | Window never armed; P3 gate open |
| Phase 6 UI | **Pass** | 2026-09-11 | Configs, Status, Settings usable |

## Known platform risks

- **Sideload + Android 15+:** SMS is a **restricted setting** (App info → Allow restricted settings).
- **RCS vs SMS:** Google Messages defaults to RCS for many contacts. `req` from requester must be forced to SMS or use future manual arm UI.
- **Android 17:** OTP-shaped / retriever SMS may be delayed up to 3 hours for non-exempt apps (targetSdk 36 today).
- **Realme/ColorOS:** Auto-start and background activity required for reliable `SMS_RECEIVED` when app is backgrounded.
- **Master switch:** OFF silences receiver entirely; ON alone does not forward without armed window.

## Proposed compatibility extension (pending owner approval)

- **Manual window arm (Phase 7):** UI button replaces SMS `req` for RCS-only requesters. Does not add RCS support; OTP sources remain SMS.

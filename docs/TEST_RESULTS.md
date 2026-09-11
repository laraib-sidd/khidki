# Test results

## Automated — 2026-09-11

| Suite | Result |
|---|---|
| `./gradlew :app:testDebugUnitTest` | **PASS** (62 tests) |
| `./gradlew :app:assembleDebug` | **PASS** |
| `bash scripts/audit_manifest.sh` | **PASS** (no INTERNET) |
| GitHub `ci` workflow on `main` | **PASS** |
| GitHub `release` workflow on `main` | **PASS** |

Environment: developer Mac + GitHub Actions `ubuntu-latest`, JDK 17, Gradle 8.13.

## Physical — partial (Realme GT 6T, 2026-09-11)

Tester: Laraib. Build: `khidki-1.1.0-debug-b*` from rolling `preview` release.

| Trial / Check | Result | Notes |
|---|---|---|
| P0 Sideload + SMS permission | **PASS** | Restricted settings flow works |
| Phase 6 UI smoke | **PASS** | Configs rule created ("Chotu Test") |
| SMS ingress (Blinkit OTP) | **PASS** | `[IN]` at 15:22, 153 chars from `CP-blnkit-S` |
| Master ON processing | **PASS** | Diagnostic events when master enabled |
| `req` via RCS (Chotu) | **FAIL (expected)** | RCS not visible to Khidki |
| OTP forward without armed window | **N/A** | `CANDIDATE: NoActiveSession` — correct behavior |
| P3 end-to-end forward | **PENDING** | Blocked: window never armed (B1) |
| P1–P2, P4–P10 formal matrix | **NOT RUN** | See `docs/PHYSICAL_TEST.md` |

### Diagnostic log samples (owner device)

```
[15:20:57] IN from ••••6497 (2 chars)
[15:20:57] CANDIDATE result: NoActiveSession
[15:22:04] IN from •••• (153 chars)        ← Blinkit OTP
[15:22:04] CANDIDATE result: NoActiveSession
```

**Interpretation:** SMS pipeline works. Forwarding blocked because no `req`/manual arm opened a window.

Do not mark OTP forwarding validated until P3 passes with an armed window.

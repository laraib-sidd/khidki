# Khidki — Session Coordination & Model Handoff Hub

> **SINGLE SOURCE OF TRUTH FOR MODEL SWITCHES & WORKER COORDINATION**
> - **Live Handover & Scratchpad:** See `docs/MODEL_MEMORY.md` for live model handover comments, traps encountered, and exact next commands.
> - Any agent, model, or worker entering this session MUST read `docs/MODEL_MEMORY.md` and this file before taking any action.
> - Entries and progress are authored as `(Laraib)`.

---

## 1. Quick Orientation

- **Project:** Khidki (खिड़की) — On-device personal SMS forwarding app for Android.
- **Repository:** Private GitHub `git@github.com:laraib-sidd/khidki.git` (`laraib-sidd/khidki`).
- **Local Directory:** `/Users/laraib.siddiqui/pers/window-sms`
- **Target Device:** Realme GT 6T (Android 16, India region, single SIM).
- **Core Security Invariant:** NO `INTERNET` permission in AndroidManifest. Ever.
- **Package ID:** `dev.laraib.khidki` (debug: `dev.laraib.khidki.debug`).

---

## 2. Current State Snapshot (2026-09-11)

| Attribute | Current Value |
|---|---|
| **Base Branch** | `main` |
| **Working Branch** | `main` |
| **Active PR** | None — all phases merged |
| **Current Phase** | **All 5 phases complete** — tagged `v1.0.0-preview` |
| **Unit Test Suite** | 62 tests passing (`./gradlew :app:testDebugUnitTest`) |
| **GitHub CI Status** | Green on PRs #1, #7, #8, #9 |
| **Latest Published APK** | `preview` release (updates on merge to `main`) |

---

## 3. Protocol for Model Switches & Worker Coordination

### When YOU enter this session (any model / worker):
1. **Read this file (`docs/COORDINATION.md`)** to inspect:
   - What phase is active
   - What git branch is checked out
   - What task is in progress
   - Any unresolved blockers or failed fixes
2. **Read `docs/DECISIONS.md`** to verify locked architectural constraints.
3. **Check git status**: `git -C /Users/laraib.siddiqui/pers/window-sms status --short`

### When YOU complete or pause a task:
1. Run local tests: `./gradlew :app:testDebugUnitTest`
2. Commit focused changes with a clear message: `git commit -m "<type>: <description>"`
3. Push branch and ensure PR is created/updated via `gh pr create` / `gh pr view`
4. Update **Section 4 (Active Execution Tracker)** and **Section 6 (Session History Log)** in this file:
   - Mark completed steps `[x]`
   - Update Current Phase and Working Branch
   - Document the latest commit SHA and PR URL
   - Note the exact next command / task for the next model or worker

---

## 4. Active Execution Tracker

- [x] **Phase 0: Baseline & Coordination Architecture**
  - [x] Audit codebase, git history, and CI status
  - [x] Establish `docs/COORDINATION.md`
  - [x] Write comprehensive multi-phase PR execution plan in `docs/plans/2026-09-11-production-hardening-and-pr-phases.md`
- [x] **Phase 1: Production Harness & Repo Standards (PR #1)**
  - Branch: `feat/production-repo-harness`
  - Scope: Multi-job CI (test, manifest audit, assemble), reproducible preview signing keystore, PR & Issue templates, CONTRIBUTING.md, Dependabot.
  - Commits: `76c7a6c`, `416175a`, `6a72a32`
- [x] **Phase 2: Sideload Onboarding & Permission Flow (PR #7)**
- [x] **Phase 3: Critical UI/UX Diagnostics & Usability (PR #8)**
- [x] **Phase 4: Production Data Layer Wiring (PR #9)**
- [x] **Phase 5: Production Hardening, Physical Runbook & Release Tag (PR #10)**
  - Branch: `feat/production-hardening-and-release`
  - Scope: StatusNotifier, PHYSICAL_TEST.md, CHANGELOG.md, `v1.0.0-preview` tag after merge.

---

## 5. Non-Negotiable Invariants & Decision Constraints

1. **No `INTERNET` permission:** The merged Android manifest must never request or merge `android.permission.INTERNET`.
2. **No single mega-commits:** Every phase is a separate git branch (`feat/...`), built with bite-sized atomic commits, submitted as a separate PR via `gh pr create`, and verified by CI before merge.
3. **No plaintext password storage:** Password verified exclusively via Keystore HMAC-SHA256 with constant-time comparison.
4. **One active window globally:** Destination is always the canonical requester. No multiple concurrent windows.
5. **Two-strike rule:** If two consecutive attempts to fix a test or build fail, STOP immediately and surface the doubt to the owner.
6. **Physical test integrity:** Never claim physical test completion without explicit verification from the owner on the Realme GT 6T.

---

## 6. Session History Log

| Timestamp (IST) | Author | Phase / Branch | Summary of Work Completed | Next Step |
|---|---|---|---|---|
| 2026-09-11 10:15 | (Laraib) | Phase 0 (`main`) | Initialized `docs/COORDINATION.md`, verified green baseline (51 unit tests), wrote execution plan. | Present execution plan to owner, obtain approval, begin Phase 1 on `feat/production-repo-harness`. |

---

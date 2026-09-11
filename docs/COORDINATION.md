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

## 2. Current State Snapshot (2026-09-11 16:15 IST)

| Attribute | Current Value |
|---|---|
| **Base Branch** | `main` |
| **Working Branch** | `feat/timed-forwarding-window` (worker creates from `main`) |
| **Active PR** | None yet |
| **Current Phase** | **Phase 7 approved** — timed forwarding window; plan: `docs/plans/2026-09-11-timed-forwarding.md` |
| **App Version** | `1.1.0-debug` (CI-driven `versionCode`) |
| **Unit Test Suite** | 62 tests passing (`./gradlew :app:testDebugUnitTest`) |
| **GitHub CI / Release** | Green; rolling `preview` release via CI-gated pipeline (`docs/RELEASE.md`) |
| **Latest APK** | GitHub Releases → **Khidki preview (rolling)** → `khidki-1.1.0-debug-b*.apk` |
| **Physical Test Status** | **Partial** — SMS receive confirmed; end-to-end forward **not yet validated** |

### Active Blockers

| ID | Blocker | Impact | Proposed resolution |
|---|---|---|---|
| **B1** | Brother (requester) can only send **RCS**, not SMS | `req` command never reaches Khidki → window never arms | **Phase 7 approved:** credential-gated timed window (≤ 2h, one config, multi-forward) — plan `docs/plans/2026-09-11-timed-forwarding.md` |
| **B2** | Owner initially expected **Master ON = auto-forward** | OTPs dropped with `NoActiveSession` even when SMS received | Document + UI copy clarifying Master ≠ bypass `req`; optional Phase 7 manual arm |
| **B3** | **RCS is invisible** to Khidki (`SMS_RECEIVED` only) | Personal chat messages (Chotu) never processed | By locked design (`docs/DECISIONS.md`); bank/Blinkit OTPs **are SMS** and work once window armed |
| **B4** | End-to-end forward **unproven** on device | P3 product gate still open | Complete test: arm window → trigger Blinkit OTP → verify forward to Chotu |

### What works on device (confirmed 2026-09-11)

- Sideload restricted-permission flow (Android 15/16)
- Phase 6 Material 3 UI (Configs, Status diagnostic stream, etc.)
- SMS receive: Blinkit OTP (`CP-blnkit-S`, 153 chars) → `[IN]` + `CANDIDATE: NoActiveSession`
- Master switch gates receiver (OFF = no `IN` lines)

### What does NOT work yet

- Forwarding OTP to requester (window never armed — `req` not received via SMS)
- RCS-based `req` from brother (architectural — no RCS support)
- History audit trail during failed candidates (`NoActiveSession` only logs to diagnostic stream, not History)

---

## 3. Protocol for Model Switches & Worker Coordination

### When YOU enter this session (any model / worker):
1. **Read this file** and **`docs/MODEL_MEMORY.md` Section 1 + latest append**.
2. **Read `docs/DECISIONS.md`** — do not reopen locked exclusions without owner approval.
3. **Check git status:** `git -C ~/pers/window-sms status --short`

### When YOU complete or pause a task:
1. Run `./gradlew :app:testDebugUnitTest` and `bash scripts/audit_manifest.sh`.
2. Commit focused changes; PR via `gh pr create` if non-trivial.
3. Update **Section 4** and **Section 6** here; **append** to `docs/MODEL_MEMORY.md` (never rewrite prior sections).

---

## 4. Active Execution Tracker

### Completed

- [x] **Phases 0–5:** Production harness, sideload onboarding, UI diagnostics, Room persistence, release tag ([PRs #1, #7–#10](https://github.com/laraib-sidd/khidki/pulls?q=is%3Apr+is%3Amerged))
- [x] **Phase 6:** Material 3 UI/UX revamp ([PR #11](https://github.com/laraib-sidd/khidki/pull/11) merged)
- [x] **Release pipeline:** CI-gated publish, traceable APK artifacts (`docs/RELEASE.md`)
- [x] **Physical partial:** SMS ingress on GT 6T; Blinkit OTP received; diagnostic stream validated

### Approved — worker starts here

- [ ] **Phase 7: Timed forwarding window** — plan: `docs/plans/2026-09-11-timed-forwarding.md`
  - Branch: `feat/timed-forwarding-window` (from `main`)
  - Locks: max 2h engine-enforced; one config armed at a time (dropdown, default first enabled);
    multi-forward with each forward logged; device-credential gate to enable; `req` path untouched;
    reboot drops window; configs become editable (number change voids credentials → regenerate)
  - Scope discipline: no `INTERNET`, no notification-listener, no RCS, no new background service

### In progress (owner)

- [ ] **Physical P3 gate:** Arm window → OTP forward → Chotu receives SMS

---

## 5. Non-Negotiable Invariants & Decision Constraints

1. **No `INTERNET` permission** — `scripts/audit_manifest.sh` fails CI if merged.
2. **SMS-in / SMS-out only** — no server, no RCS API, no notification-listener SMS (locked).
3. **Master ON ≠ auto-forward** — engine listens; `req` (or future manual arm) opens window.
4. **One active window globally** — destination = canonical requester.
5. **Two-strike rule** — two failed fix attempts → stop and ask owner.
6. **Physical claims** — only owner marks device tests pass/fail.
7. **Phase 7 timed window (locked 2026-09-11):** max 2h engine-enforced; one config at a time;
   credential-gated enable; each forward logged; `req` single-forward semantics unchanged;
   destination always the armed config's requester.

---

## 6. Session History Log

| Timestamp (IST) | Author | Phase / Branch | Summary | Next Step |
|---|---|---|---|---|
| 2026-09-11 10:15 | (Laraib) | Phase 0 | Coordination hub + execution plan | Phase 1 |
| 2026-09-11 10:45 | (Laraib) | Phases 1–5 | PRs #1, #7–#10 merged; `v1.0.0-preview` tag | Physical test |
| 2026-09-11 12:45 | (Laraib) | Phase 6 plan | UI/UX audit; planner/worker accountability | Implement Phase 6 |
| 2026-09-11 13:07 | (Laraib) | Phase 6 | PR #11 merged | Owner re-test |
| 2026-09-11 14:30 | (Laraib) | Release fix | CI-gated pipeline; `1.1.0` traceable APKs | Install + test |
| 2026-09-11 15:35 | (Laraib) | Physical test | SMS works; RCS `req` blocked; `NoActiveSession` understood | Owner approves Phase 7 manual arm |
| 2026-09-11 16:15 | (Laraib) | Phase 7 plan | Timed window approved (2h max, multi-forward, one config, editable rules); plan written; RCS/WhatsApp/Telegram channels evaluated and rejected | Worker implements Tasks 0–7 on `feat/timed-forwarding-window` |

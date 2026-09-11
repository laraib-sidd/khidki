# Timed Forwarding Window — Execution Plan (Phase 7)

Date: 2026-09-11. Owner: Laraib. Status: awaiting owner approval.
Worker: read `WORKER.md`, `docs/DECISIONS.md`, this plan, then execute task-by-task.

## Goal

A **timed forwarding switch**: owner enables it on the Status tab with a duration,
and until expiry every SMS matching the armed config is forwarded to that config's
requester. No `req` SMS needed. Auth = physical possession (device credential gate).
The `req` path, lockout logic, and no-`INTERNET` invariant are untouched.

## Locked decisions (owner, 2026-09-11)

| # | Decision |
|---|---|
| 1 | Max duration **2 hours** (7_200 s), enforced **engine-side**, not just UI. Presets: 15m / 30m / 1h / 2h. |
| 2 | **Multi-forward**: window stays open across forwards until expiry/cancel. **Each forward logged** (existing `CANDIDATE_FORWARDED` per event). |
| 3 | **One config armed at a time.** UI selects which config (dropdown, default = first enabled). Configs differ by requester number — the armed config's requester is the sole destination (locked invariant: destination = canonical requester, never a free field). |
| 4 | Enabling requires **device credential** (BiometricPrompt with device-credential fallback). |
| 5 | A `req` arriving during a timed window returns existing `IgnoredActiveSession` (single global session invariant holds). No ack SMS on timed arm (owner holds the phone). |
| 6 | Reboot drops the window (existing `bootId` invalidation path — free). |
| 7 | Blast radius = existing budget: each forward independently reserved, 20 parts / 24h cap. |
| 8 | Configs become **editable**: label / regexes / window editable in place; changing the **requester number voids credentials** → must regenerate via the existing save-and-generate flow. |

## Architecture delta (minimal)

- `AuthorizationSession` gains `origin: SessionOrigin { REQUEST, TIMED }` (default `REQUEST`
  for backward compat) and `forwardCount: Int = 0`.
- `ForwardingEngine.handleCandidate`: for `TIMED` sessions, a successful forward returns the
  session to `ARMED` (not `SUBMITTED`), increments `forwardCount`, records `CANDIDATE_FORWARDED`.
  `REQUEST` sessions keep exact current behavior (one-and-done). Expiry/claim/submit guards unchanged.
- `ForwardingEngine.armTimedWindow(configId, durationSeconds, nowMillis)`: rejects when
  config missing/disabled, another session active, or duration outside 60..7_200 s.
  Records `TIMED_ARMED`. `cancelTimedWindow()` records `TIMED_CANCELLED`; expiry records `TIMED_EXPIRED`.
- Room: new columns on the session table (`origin`, `forwardCount`) + migration test.
  (Worker: read `data/db/entities/SessionEntity.kt`, `data/mapping/EntityMappers.kt` first.)
- `AuditEventType` += `TIMED_ARMED`, `TIMED_CANCELLED`, `TIMED_EXPIRED`. History labels in `UiUtils`.
- UI: `TimedForwardingCard` on Status (config dropdown, duration chips, credential-gated switch,
  reuse Phase 6 countdown). `EditRuleBottomSheet` on Configs (reuse add-sheet + validation).

## Invariants (CI must prove these)

- Merged manifest contains no `INTERNET` (`bash scripts/audit_manifest.sh`).
- Duration > 7_200 s rejected engine-side even if UI is bypassed (unit test).
- Forward destination always equals the armed config's requester (unit test).
- `req`-origin sessions still terminate after exactly one forward (existing tests stay green).

---

### Task 0: Pre-read (no code)

**Files (read-only):** `docs/DECISIONS.md`, `domain/model/DomainTypes.kt`,
`domain/session/ForwardingEngine.kt`, `data/db/entities/SessionEntity.kt`,
`data/mapping/EntityMappers.kt`, `ui/KhidkiViewModel.kt`, `ui/screens/StatusScreen.kt`,
`ui/screens/ConfigsScreen.kt`, `ui/components/UiUtils.kt`

**Steps:**
1. Read the files above; note exact field/constructor shapes you will extend.
2. Confirm where audit events become History rows (trace `AuditStore` → History).
3. Verify whether `KhidkiViewModel` is JVM-unit-testable (no existing `KhidkiViewModelTest`
   in `app/src/test` — if Robolectric/Androidx blocks JVM tests, test at engine level +
   verify UI manually; do NOT add a broken test target).

### Task 1: Domain — TIMED origin + multi-forward

**Files:**
- Modify: `domain/model/DomainTypes.kt` (`SessionOrigin`, `AuthorizationSession.origin/forwardCount`, `AuditEventType.TIMED_*`)
- Modify: `domain/session/ForwardingEngine.kt` (`armTimedWindow`, `cancelTimedWindow`, multi-forward branch in `handleCandidate`, `TIMED_EXPIRED` in `refreshSessions`/expiry path)
- Test: `app/src/test/java/dev/laraib/khidki/domain/session/ForwardingEngineTest.kt` (extend)

**Steps:**
1. Add failing tests: timed arm happy path; duration 7_201 s rejected; second forward in timed window succeeds and stays `ARMED` with `forwardCount == 2`; `REQUEST` session still `SUBMITTED` after one forward; `req` during timed window → `IgnoredActiveSession`; cancel → `CANCELLED`; expiry → `TIMED_EXPIRED`.
2. Run, verify fail for the right reason: `./gradlew :app:testDebugUnitTest --tests "*ForwardingEngineTest*"`
3. Implement minimal changes to pass.
4. Re-run full engine tests green.
5. Commit: `feat(domain): add TIMED session origin with multi-forward window`

### Task 2: Data — persist origin + forwardCount

**Files:**
- Modify: `data/db/entities/SessionEntity.kt`, `data/mapping/EntityMappers.kt`,
  `data/db/KhidkiDatabase.kt` (version bump + migration)
- Test: `app/src/test/java/dev/laraib/khidki/data/db/KhidkiDatabaseTest.kt` (migration test)

**Steps:**
1. Write failing migration test (old row → defaults `REQUEST` / 0; new row round-trips).
2. Implement columns + migration + mapper changes.
3. `./gradlew :app:testDebugUnitTest --tests "*KhidkiDatabaseTest*"` green.
4. Commit: `feat(data): persist timed session origin and forward count`

### Task 3: ViewModel — timed arm state + actions

**Files:**
- Modify: `ui/KhidkiViewModel.kt` (`armTimedWindow(configId, durationSeconds)`, `cancelTimedWindow()`, selected-config + duration UI state, countdown source reuse)
- Test: engine-level coverage from Task 1; add `ui/KhidkiViewModelTest.kt` only if JVM-testable (see Task 0.3)

**Steps:**
1. Wire ViewModel → engine/container timed actions; expose active timed session + remaining millis.
2. `./gradlew :app:testDebugUnitTest` green.
3. Commit: `feat(ui): wire timed window arm and cancel actions`

### Task 4: UI — TimedForwardingCard on Status

**Files:**
- Create: `ui/components/TimedForwardingCard.kt`
- Modify: `ui/screens/StatusScreen.kt`, `res/values/strings.xml`
- (Credential gate: BiometricPrompt with `DEVICE_CREDENTIAL` fallback before calling arm.)

**Steps:**
1. Card: config dropdown (enabled configs, first = default), duration chips (15/30/60/120m),
   switch (credential-gated on enable), live countdown (reuse Phase 6 component), cancel button.
2. No-config state: muted hint pointing to Configs tab (never crash on empty list).
3. `./gradlew :app:testDebugUnitTest :app:assembleDebug` green.
4. Commit: `feat(ui): add credential-gated timed forwarding card`

### Task 5: Configs — editable rules

**Files:**
- Modify: `ui/KhidkiViewModel.kt` (`updateConfiguration`: same RE2 + 20-limit validation as save),
  `ui/screens/ConfigsScreen.kt` (`EditRuleBottomSheet` reusing add-sheet, edit affordance per card)
- Rule: label/regexes/window edit in place; **requester-number change voids credentials** and
  routes through the existing save-and-generate command flow (reveal dialog + one-tap copy).

**Steps:**
1. Implement edit sheet + ViewModel update (number-change → credential void + regenerate).
2. `./gradlew :app:testDebugUnitTest :app:assembleDebug` green.
3. Commit: `feat(ui): make forwarding rules editable with credential void on number change`

### Task 6: History labels + notifier

**Files:**
- Modify: `ui/components/UiUtils.kt` (`TIMED_ARMED` → "Timed Window Opened" etc.),
  `ui/screens/HistoryScreen.kt` (badges), `platform/notification/StatusNotifier.kt`
  (notify on timed arm + each timed forward, graceful when permission denied)

**Steps:**
1. Labels + badges + notifier hooks.
2. `./gradlew :app:testDebugUnitTest :app:assembleDebug && bash scripts/audit_manifest.sh` green.
3. Commit: `feat(ui): label timed events in history and notifications`

### Task 7: Docs, verify, PR

**Files:**
- Modify: `docs/ARCHITECTURE.md` (timed-window section), `docs/PHYSICAL_TEST.md`
  (timed trials: arm 15m → Blinkit OTP → 2nd OTP → expiry → cancel), `CHANGELOG.md`
  (Unreleased), `docs/COORDINATION.md` (§2/§4), append `docs/MODEL_MEMORY.md` (append-only)

**Steps:**
1. `./gradlew :app:testDebugUnitTest --quiet` — all green.
2. `bash scripts/audit_manifest.sh` — PASS.
3. Push branch `feat/timed-forwarding-window`, `gh pr create` vs `main`, CI green, request review.
4. Append completion log to `docs/MODEL_MEMORY.md`.

## Definition of done

- Timed window arms exactly one selected config for ≤ 2h behind device credential; `req` path untouched.
- Multiple forwards each logged; budget enforced per forward; expiry/cancel/reboot all terminate.
- Configs editable; number change voids credentials and regenerates.
- All unit tests + manifest audit green; PR open with physical-test trials documented.
- No `INTERNET`, no new background service, no notification-listener, no RCS scope creep.

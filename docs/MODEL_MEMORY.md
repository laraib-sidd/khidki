# Khidki — Model Handover & Live Memory Log

> **FOR ALL MODELS & WORKERS (Claude, Gemini, GPT, Cursor, etc.):**
> This file is the shared live memory scratchpad across model switches.
> 
> **THE PROTOCOL:**
> 1. **When you wake up / switch in:** Read this file FIRST. Look at **Section 1 (Live State Snapshot)** and **Section 2 (Latest Handover Note)**. You will know exactly what the previous model was doing, what traps it encountered, and what command you must run right now.
> 2. **When the user says they are switching the model (or before you yield control):**
>    - Run `./gradlew :app:testDebugUnitTest --quiet` to check if tests are clean.
>    - Check `git status --short`.
>    - Update **Section 1** with your exact current state.
>    - Write a new entry in **Section 2** detailing what you just did, any tricky bugs/gotchas you noticed, and the exact next step for the incoming model.
>    - Append a row to **Section 3 (Handover History)**.

---

## 1. Live State Snapshot (Always reflects the very last active moment)

| Key | Current Value |
|---|---|
| **Last Active Model** | Composer (Coordinator) |
| **Timestamp** | 2026-09-11 10:28 IST |
| **Git Branch** | `feat/sideload-onboarding-permission-flow` (commit `3bfb4ac`) |
| **Working Tree Status** | Clean — Phase 2 code complete, PR #2 pending |
| **Active Plan** | `docs/plans/2026-09-11-production-hardening-and-pr-phases.md` |
| **Active Phase / PR** | **Phase 2 complete** — awaiting PR #2 merge |
| **Local Unit Tests** | 54 / 54 passing (`./gradlew :app:testDebugUnitTest`) |
| **CI / Release Status** | PR #1 merged; PR #2 CI pending |
| **Blockers / Doubts** | None. |

---

## 2. Latest Handover Note (Comments & Gotchas for the Incoming Model)

### From: Composer (2026-09-11 10:28 IST)
### To: Incoming Model (Next turn or Worker)

#### Context & Where We Are:
- **Phase 1 MERGED** (PR #1). Production CI, deterministic keystore, repo standards live on `main`.
- **Phase 2 DONE** on `feat/sideload-onboarding-permission-flow`:
  1. `ac9f530` — `PermissionGate` helper + tests
  2. `3bfb4ac` — sideload pre-flight card, removed onCreate permission auto-prompt
- PR #2 pending merge. **Next: Phase 3** (`feat/ui-diagnostics-and-usability`) after PR #2 merges.

#### Tricky Things / Gotchas You Must Know:
1. **Permission flow order matters:** User must tap "Open App Info" FIRST, enable restricted settings, THEN "Grant SMS Permissions". Don't re-add auto-prompt on launch.
2. **`PermissionGate.createAppDetailsIntent`** uses `FLAG_ACTIVITY_NEW_TASK` for safety from non-activity contexts.
3. **Phase 3 scope:** Event log on Status tab, copy command button, real session cancel, RE2 validation, countdown timer.

#### Your EXACT Next Steps:
1. Merge PR #2 after CI green: `gh pr merge 2 --repo laraib-sidd/khidki --merge`
2. `git checkout main && git pull && git checkout -b feat/ui-diagnostics-and-usability`
3. Start Phase 3 Task 3.1: `DiagnosticEventBus` + wire into `SmsInboundProcessor`.

---

## 3. Handover History Log

| Timestamp (IST) | Outgoing Model | Incoming Model | Branch / Phase | Last Action Completed | Next Action for Incoming | Key Comments / Warnings |
|---|---|---|---|---|---|---|
| 2026-09-11 10:15 | Gemini | Pending Switch | `main` / Phase 0 | Wrote execution plan and `docs/MODEL_MEMORY.md`. | Create `feat/production-repo-harness` and execute Task 1.1. | All 51 tests green. |
| 2026-09-11 10:20 | Composer | — | `feat/production-repo-harness` / Phase 1 | Completed Phase 1, opened PR #1, CI green, merged. | Start Phase 2. | Keystore at `app/debug-keystore/debug.keystore`. |
| 2026-09-11 10:28 | Composer | Pending Switch | `feat/sideload-onboarding-permission-flow` / Phase 2 | Phase 2 done (2 commits), 54 tests pass. | Open/merge PR #2, start Phase 3. | No auto SMS prompt on launch anymore. |

---

## 4. Scratchpad & Working Notes (Freeform for current model)

- *Tip for worker:* Keep PR bodies informative and use `gh pr create` with heredoc strings matching the templates we will introduce in Phase 1.
- *Tip for test running:* Always verify `:app:testDebugUnitTest` passes before committing.
- *Tip for Room/KSP:* Room schema directory is configured at `app/schemas`. If Room entities change in Phase 4, KSP will generate updated schema files there.

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
| **Timestamp** | 2026-09-11 10:20 IST |
| **Git Branch** | `feat/production-repo-harness` (commit `6a72a32`) |
| **Working Tree Status** | Clean — Phase 1 code complete, PR pending |
| **Active Plan** | `docs/plans/2026-09-11-production-hardening-and-pr-phases.md` |
| **Active Phase / PR** | **Phase 1 complete** — awaiting PR #1 merge |
| **Local Unit Tests** | 51 / 51 passing (`./gradlew :app:testDebugUnitTest`) |
| **CI / Release Status** | PR CI pending (old `preview-apk` workflow replaced) |
| **Blockers / Doubts** | None. |

---

## 2. Latest Handover Note (Comments & Gotchas for the Incoming Model)

### From: Composer (2026-09-11 10:20 IST)
### To: Incoming Model (Next turn or Worker)

#### Context & Where We Are:
- **Phase 1 is DONE** on branch `feat/production-repo-harness` with 3 atomic commits:
  1. `76c7a6c` — deterministic debug keystore + signing config
  2. `416175a` — `ci.yml`, `release.yml`, `scripts/audit_manifest.sh` (deleted `apk.yml`)
  3. `6a72a32` — PR/issue templates, Dependabot, CONTRIBUTING, coordination docs
- PR #1 needs to be pushed/created (or merged if already done when you read this).
- **Next phase after merge:** Phase 2 on `feat/sideload-onboarding-permission-flow`.

#### Tricky Things / Gotchas You Must Know:
1. **`.gitignore` keystore exception:** `*.keystore` is ignored globally, but `!app/debug-keystore/debug.keystore` is explicitly allowed. Don't remove the negation.
2. **Manifest audit paths:** `scripts/audit_manifest.sh` checks source manifest AND merged debug manifest. Run after `assembleDebug`.
3. **Two workflows now:** `ci.yml` runs on PRs; `release.yml` runs on push to `main` and publishes `preview` APK.
4. **Android 15/16 Restricted Settings (Phase 2):** Still the top UX blocker for physical testing.

#### Your EXACT Next Steps:
1. Check if PR #1 is merged: `gh pr list --repo laraib-sidd/khidki --state all`
2. If merged → `git checkout main && git pull` → create `feat/sideload-onboarding-permission-flow`
3. If not merged → wait for CI green, then merge PR #1 before starting Phase 2.

---

## 3. Handover History Log

| Timestamp (IST) | Outgoing Model | Incoming Model | Branch / Phase | Last Action Completed | Next Action for Incoming | Key Comments / Warnings |
|---|---|---|---|---|---|---|
| 2026-09-11 10:15 | Gemini | Pending Switch | `main` / Phase 0 | Wrote execution plan and `docs/MODEL_MEMORY.md`. | Create `feat/production-repo-harness` and execute Task 1.1. | All 51 tests green. |
| 2026-09-11 10:20 | Composer | Pending Switch | `feat/production-repo-harness` / Phase 1 | Completed Phase 1 (3 commits). Tests + manifest audit pass. | Push branch, open PR #1, merge after CI green, start Phase 2. | Keystore committed at `app/debug-keystore/debug.keystore`. |

---

## 4. Scratchpad & Working Notes (Freeform for current model)

- *Tip for worker:* Keep PR bodies informative and use `gh pr create` with heredoc strings matching the templates we will introduce in Phase 1.
- *Tip for test running:* Always verify `:app:testDebugUnitTest` passes before committing.
- *Tip for Room/KSP:* Room schema directory is configured at `app/schemas`. If Room entities change in Phase 4, KSP will generate updated schema files there.

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
| **Last Active Model** | Gemini (Parent / Coordinator) |
| **Timestamp** | 2026-09-11 10:15 IST |
| **Git Branch** | `main` (commit `89ec2d7`) |
| **Working Tree Status** | Clean (untracked plan & coordination docs) |
| **Active Plan** | `docs/plans/2026-09-11-production-hardening-and-pr-phases.md` |
| **Active Phase / PR** | Ready to begin **Phase 1: Production Harness & Repo Standards (PR #1)** |
| **Local Unit Tests** | 51 / 51 passing (`./gradlew :app:testDebugUnitTest`) |
| **CI / Release Status** | CI Green (Run `34514006117`), `preview` release live |
| **Blockers / Doubts** | None. Waiting for Phase 1 branch creation. |

---

## 2. Latest Handover Note (Comments & Gotchas for the Incoming Model)

### From: Gemini (2026-09-11 10:15 IST)
### To: Incoming Model (Next turn or Worker)

#### Context & Where We Are:
- Khidki v1 core is complete and tested (51 unit tests). The user wants us to harden the repository to production standards and address critical sideload/UI gaps before physical testing on the **Realme GT 6T**.
- We have created a 5-Phase, 5-PR execution plan in `docs/plans/2026-09-11-production-hardening-and-pr-phases.md`.
- **CRITICAL INVARIANT:** Under NO circumstances should `android.permission.INTERNET` ever be added to any manifest or library.
- **WORKFLOW INVARIANT:** Work must be done in feature branches (`feat/...`), with atomic commits, submitted via `gh pr create` as PRs to `main`, and merged only after local tests and CI pass. No mega-commits.

#### Tricky Things / Gotchas You Must Know:
1. **Gradle / Local Environment:**
   - Gradle wrapper `./gradlew` is working cleanly with Java 17 (`sourceCompatibility` 17, `jvmTarget` 17).
   - Fast unit test command: `./gradlew :app:testDebugUnitTest --quiet` (takes ~3s).
   - Compiler log files like `.kotlin/` were recently gitignored. Don't touch `.gitignore` unless adding intentional ignores.
2. **Deterministic Keystore (Phase 1):**
   - The current GitHub Actions workflow (`apk.yml`) creates an ephemeral debug keystore every run. If you install an APK today and an updated APK tomorrow, Android will refuse to update because signatures mismatch.
   - In Phase 1, we must add a deterministic debug keystore (`app/debug-keystore/debug.keystore`) so every build is signed identically.
3. **Android 15/16 Restricted Settings (Phase 2):**
   - Sideloaded APKs on Android 15/16 silently deny SMS permissions if `permissionLauncher.launch` is called on launch. The user must first go to `App Info -> ⋮ -> Allow restricted settings`. Phase 2 handles this via a pre-flight UI and deep link.
4. **Author tag:**
   - As per workspace rules, diary and coordination entries are authored as `(Laraib)`.

#### Your EXACT Next Steps:
1. Create and switch to branch `feat/production-repo-harness`:
   ```bash
   git checkout -b feat/production-repo-harness
   ```
2. Start **Phase 1, Task 1.1** from `docs/plans/2026-09-11-production-hardening-and-pr-phases.md`:
   - Generate `app/debug-keystore/debug.keystore`.
   - Update `app/build.gradle.kts` to sign debug builds with this keystore.
   - Run `./gradlew :app:assembleDebug` to verify.
   - Make an atomic commit.

---

## 3. Handover History Log

| Timestamp (IST) | Outgoing Model | Incoming Model | Branch / Phase | Last Action Completed | Next Action for Incoming | Key Comments / Warnings |
|---|---|---|---|---|---|---|
| 2026-09-11 10:15 | Gemini | Pending Switch | `main` / Phase 0 | Wrote `docs/plans/2026-09-11-production-hardening-and-pr-phases.md` and set up `docs/MODEL_MEMORY.md`. | Create `feat/production-repo-harness` and execute Task 1.1. | All 51 tests green. Do not mega-commit; use discrete PRs. |

---

## 4. Scratchpad & Working Notes (Freeform for current model)

- *Tip for worker:* Keep PR bodies informative and use `gh pr create` with heredoc strings matching the templates we will introduce in Phase 1.
- *Tip for test running:* Always verify `:app:testDebugUnitTest` passes before committing.
- *Tip for Room/KSP:* Room schema directory is configured at `app/schemas`. If Room entities change in Phase 4, KSP will generate updated schema files there.

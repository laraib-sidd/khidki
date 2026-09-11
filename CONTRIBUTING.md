# Contributing to Khidki

Personal sideloaded Android app. Not on Play Store. All rights reserved.

## Branching

- Base branch: `main`
- Feature branches: `feat/<short-description>`
- Fix branches: `fix/<short-description>`
- One logical change per PR. No mega-commits.

## Local Development

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
bash scripts/audit_manifest.sh
```

Requirements: Java 17, Android SDK (compileSdk 36).

## Commits

Use conventional prefixes:

- `feat:` new behavior
- `fix:` bug fix
- `build:` Gradle / signing / toolchain
- `ci:` GitHub Actions
- `docs:` documentation only
- `test:` tests only
- `chore:` maintenance

## Pull Requests

1. Branch from latest `main`.
2. Keep PRs focused and reviewable.
3. Fill in `.github/pull_request_template.md`.
4. Ensure CI passes (`ci` workflow).
5. Do not merge with failing tests or manifest audit.

## Security Invariants (non-negotiable)

- **Never** add `android.permission.INTERNET`.
- **Never** store plaintext passwords — Keystore HMAC only.
- **Never** use live bank OTPs in tests.
- Read locked decisions in `docs/DECISIONS.md` before changing product behavior.

## Model / Worker Coordination

- Live handover: `docs/MODEL_MEMORY.md` (append-only for history sections)
- Phase tracker + **blockers**: `docs/COORDINATION.md`
- Physical test status: `docs/TEST_RESULTS.md`, `docs/PHYSICAL_TEST.md`
- Execution plans: `docs/plans/`

Update `docs/MODEL_MEMORY.md` before switching models or ending a work session.

## Current blockers (2026-09-11)

See `docs/COORDINATION.md` Section 2. Summary: SMS ingress works; end-to-end forward blocked because requester uses RCS for `req`. Phase 7 manual arm proposed.

## Releases

See `docs/RELEASE.md` for the full runbook.

- **Auto releases**: every `main` push creates tag `v<version>-b<N>` + GitHub Release (e.g. `v1.1.0-b17`).
- **Manual milestones**: push a `v*` git tag (e.g. `v1.2.0`) to publish at that tag.
- APKs are named `khidki-<version>-debug-b<build>-<sha>.apk` with `.sha256` checksums.
- Install target: **Latest** on GitHub Releases; record the `v*-b<N>` tag in physical test notes.
- Debug APKs use the committed deterministic keystore in `app/debug-keystore/` unless `KHIDKI_PREVIEW_KEYSTORE_BASE64` is set in GitHub secrets.

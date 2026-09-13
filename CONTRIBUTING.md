# Contributing to Khidki

FOSS Android SMS forwarder (GPL-3.0-or-later). Not on Play Store. GitHub Releases and F-Droid.

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

## Releases

See `docs/RELEASE.md` for the full runbook.

- **Public releases**: push a `v*` git tag (e.g. `v1.0.0`) after CI secrets are configured.
- CI publishes a signed GitHub Release only for tag pushes.
- APKs are named `khidki-<version>-b<code>-<sha>.apk` with `.sha256` checksums.
- Install target: **Latest** on GitHub Releases.
- Local `assembleRelease` may fall back to the debug keystore when no release keystore is present.

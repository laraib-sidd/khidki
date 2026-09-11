# Changelog

All notable changes to Khidki are documented here.

## [1.0.0-preview] — 2026-09-11

### Added
- Production CI/CD with PR validation, manifest security audit, and split release workflow
- Deterministic debug keystore for sideload APK updates without reinstall
- Sideload permission pre-flight card with App Info deep link (Android 15/16)
- Live diagnostic event log on Status tab for on-device debugging
- One-tap copy for generated `req` commands
- Real session cancellation with Cancel Active Window button
- RE2 regex validation and 20-configuration limit in UI
- Persistent Room-backed lockouts, SMS budget, and duplicate fingerprint dedup
- Graceful status notifications when window arms or SMS forwards
- Coordination docs: `docs/COORDINATION.md`, `docs/MODEL_MEMORY.md`

### Security
- Manifest audit fails CI if `INTERNET` permission is merged
- Lockout and daily SMS part budget survive device reboot

## [Unreleased]

### Added
- Phase 6 Material 3 UI/UX revamp (Status hero card, Config bottom sheet, History badges, Settings regex tester)
- Production-standard release pipeline: CI-gated publish, traceable APK names, SHA256 checksums, release metadata

### Changed
- Version bumped to `1.1.0` with CI-driven `versionCode` for reliable sideload updates
- Rolling `preview` release is now the default Latest install target

## [1.1.0-preview] — 2026-09-11

- Phase 6 UI/UX revamp merged to `main`
- Production-standard CI-gated release pipeline with traceable APK artifacts

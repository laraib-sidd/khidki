# Changelog

All notable changes to Khidki are documented here.

## [Unreleased]

### Physical testing (2026-09-11, Realme GT 6T)

- **Pass:** Sideload permissions, Phase 6 UI, SMS receive (Blinkit OTP), diagnostic stream
- **Blocked:** End-to-end OTP forward — requester uses RCS only; `req` never received
- **Clarified:** Master ON ≠ auto-forward; `NoActiveSession` when OTP arrives without armed window
- **Proposed:** Phase 7 manual "Open window" button for RCS-only requester scenario

### Added (pending release tag)

- **Phase 7:** Timed forwarding window on Status tab (credential-gated, 15m–2h, multi-forward, one config at a time)
- **Phase 7:** Editable forwarding rules; requester number change voids credentials and regenerates access code
- Phase 6 Material 3 UI/UX revamp (Status hero card, Config bottom sheet, History badges, Settings regex tester)
- Production-standard release pipeline: CI-gated publish, traceable APK names, SHA256 checksums, release metadata

### Changed

- Release pipeline: single `ci` workflow publishes `v<version>-b<N>` GitHub Release on every `main` push; removed `preview` rolling tag
- Room DB v2: `sessions.origin` + `sessions.forwardCount` for TIMED session persistence
- Version `1.1.0` with CI-driven `versionCode` for reliable sideload updates
- Rolling `preview` release is the install target (`docs/RELEASE.md`)

## [1.1.0-preview] — 2026-09-11

### Added
- Material 3 UI overhaul ([PR #11](https://github.com/laraib-sidd/khidki/pull/11))
- CI-gated release workflow with `khidki-*-debug-b*-*.apk` artifacts
- `docs/RELEASE.md` deployment runbook

### Changed
- App version `1.0.0` → `1.1.0`
- Dependabot removed (closed bot PRs #2–#6)

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

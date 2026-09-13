# Changelog

All notable changes to Khidki are documented here.

## [Unreleased]

## [1.0.2] — 2026-09-13

### Fixed
- Omit AGP dependency-metadata signing block so F-Droid `check apk` accepts the APK.

## [1.0.1] — 2026-09-13

### Fixed
- Release builds no longer require a `signingConfigs.release` block, so F-Droid `assembleRelease` works without the owner keystore.

## [1.0.0] — 2026-09-13

First public FOSS release.

### Added
- On-device SMS forwarding to up to five people with per-person filters (Shopping, Banks, UPI, Government, alerts, All SMS, named senders)
- Timed forwarding windows (15 minutes to 2 hours, or Until I stop)
- Ongoing notification with Stop forwarding
- GPL-3.0-or-later license
- Fastlane listing metadata and draft F-Droid build recipe (`docs/fdroid/dev.laraib.khidki.yml`)

### Security
- No `INTERNET` permission
- Manifest audit fails CI if network permission is merged

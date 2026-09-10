# Khidki

Personal Android app: an authenticated SMS command opens a short window and forwards matching OTP-shaped messages back to the same trusted number. No server. Not on Google Play.

This repository is private. Sideload the `preview` APK from GitHub Releases. GitHub Releases is not an app store, so Android 15+ still treats SMS as a restricted setting.

## For the worker

Start at [`WORKER.md`](WORKER.md). Locked product decisions: [`docs/DECISIONS.md`](docs/DECISIONS.md). Execution tasks: [`docs/plans/2026-09-10-khidki-execution.md`](docs/plans/2026-09-10-khidki-execution.md).

## For testers (Laraib / brother)

After CI publishes a release, install from https://github.com/laraib-sidd/khidki/releases/tag/preview and follow [`docs/PHYSICAL_TEST.md`](docs/PHYSICAL_TEST.md). Do not use live bank OTPs.

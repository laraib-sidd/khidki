# Khidki

Personal Android app: send `req <8-digit-password>` from a trusted number to open a short window; matching OTP-shaped SMS is forwarded back to that same number. No server.

**Not on Google Play.** Install the debug APK from [GitHub Releases `preview`](https://github.com/laraib-sidd/khidki/releases/tag/preview) (private repo — Laraib shares the file).

## Install (sideload)

1. Download `app-debug.apk` from the `preview` release.
2. Install (uninstall previous preview if signing changed).
3. **Settings → Apps → Khidki → ⋮ → Allow restricted settings** (Android 15+).
4. **Permissions → SMS → Allow**.
5. Realme: **Auto-start** on, **Allow background activity**, battery unrestricted.
6. Open Khidki, grant SMS if prompted, enable master switch.

## Use

1. **Configs** → add label, requester number (`+91…`), sender regex, content regex (e.g. `OTP`).
2. Tap **Save & generate command** — copy `req XXXXXXXX` now (shown once).
3. From the requester phone, SMS the command **before** the OTP you want forwarded.
4. **Status** shows active window; matching SMS forwards automatically.

## Development

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Docs: `docs/ARCHITECTURE.md`, `docs/SECURITY.md`, `docs/PRIVACY.md`, `docs/COMPATIBILITY.md`, `docs/PHYSICAL_TEST.md`.

Worker entry: `WORKER.md`. Locked decisions: `docs/DECISIONS.md`.

**Never use live bank OTPs in tests.**

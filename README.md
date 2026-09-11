# Khidki

Personal Android app: a trusted requester opens a short forwarding window; matching OTP-shaped **SMS** is forwarded back to that same number. No server.

**Not on Google Play.** Install from [GitHub Releases → Khidki preview (rolling)](https://github.com/laraib-sidd/khidki/releases/tag/preview) (private repo).

## Install (sideload)

1. Download `khidki-1.1.0-debug-b*.apk` from the **preview** release (see `docs/RELEASE.md`).
2. Install over existing build (same debug keystore; higher `versionCode` required).
3. **Settings → Apps → Khidki → ⋮ → Allow restricted settings** (Android 15+).
4. **Permissions → SMS → Allow**.
5. Realme: **Auto-start** on, **Allow background activity**, battery **Unrestricted**.
6. Open Khidki → enable **Master** switch on Status tab.

## How it works

| Step | What happens |
|---|---|
| **Master ON** | Engine listens for SMS. Does **not** auto-forward. |
| **`req <password>`** | Requester sends SMS command → opens **2-minute window**. |
| **OTP SMS arrives** | If it matches filters → forwarded to requester. |

**Master ON does not replace `req`.** Both are required.

### SMS only — not RCS

Khidki reads **SMS** via `SMS_RECEIVED`. **RCS chat messages are invisible.**

- Bank/Blinkit OTPs → SMS → **works** (once window is armed).
- Google Messages RCS chat with a contact → **does not work** for `req`.

If your requester can only use RCS, see **Blockers** in `docs/COORDINATION.md` (proposed manual arm UI).

## Use

1. **Configs** → **+** → label, requester `+91…`, sender/content regex → **Save & generate access code**.
2. Copy `req <password>` — shown once.
3. Requester sends that command as **SMS** (not RCS) **before** the OTP.
4. **Status** shows active window + countdown. Matching OTP SMS forwards automatically.
5. Watch **Status → diagnostic stream** for `[IN]`, `[CMD]`, `[CANDIDATE]` during testing.

## Development

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
bash scripts/audit_manifest.sh
```

## Docs

| Doc | Purpose |
|---|---|
| `docs/COORDINATION.md` | Phase tracker, **blockers**, handoff |
| `docs/MODEL_MEMORY.md` | Append-only model memory log |
| `docs/PHYSICAL_TEST.md` | Device test matrix + SMS/RCS notes |
| `docs/TEST_RESULTS.md` | Automated + physical results |
| `docs/RELEASE.md` | CI/CD and APK install |
| `docs/DECISIONS.md` | Locked product/security decisions |

**Never use live bank OTPs in synthetic tests.**

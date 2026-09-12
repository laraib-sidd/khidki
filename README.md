# Khidki

Personal Android app: forward OTP-shaped **SMS** to a trusted number during a short window you control. Everything stays on your phone — no server, no internet permission.

**Not on Google Play.** Install from [GitHub Releases](https://github.com/laraib-sidd/khidki/releases) → **Latest** (`v1.3.0-b<N>` per `main` push).

## Install

See **`docs/INSTALL.md`** for the full guide. Short version:

1. Download `khidki-1.3.0-b*.apk` from GitHub Releases (Latest).
2. If upgrading from an old debug build (`dev.laraib.khidki.debug`), uninstall it first.
3. **App info → ⋮ → Allow restricted settings** (Android 15+), then grant SMS + notifications.
4. Realme / ColorOS: **Auto-start** on, battery **Unrestricted**.
5. Open Khidki → complete the welcome steps → turn **Forwarding** on on the Home tab.

## How it works

| Step | What happens |
|---|---|
| **Forwarding ON** | Khidki listens for SMS. Does **not** auto-forward. |
| **Arm a window** | Timed arm (in app) or `req <password>` SMS from requester |
| **OTP SMS arrives** | If it matches your rule → forwarded to the requester |

### SMS only — not RCS

Khidki reads **SMS**, not RCS chat. Bank and delivery OTPs that arrive as SMS work. Google Messages RCS chats do not.

## Use

1. **Rules** → **+** → label, requester `+91…`, filters → save (access command shown once).
2. **Home** → arm a timed window, or have the requester send the `req` command by SMS.
3. Matching OTP SMS forwards automatically during the open window.
4. **History** shows audit events; **Settings** has permissions and privacy info.

## Development

```bash
./gradlew :app:testDebugUnitTest :app:assembleRelease
bash scripts/audit_manifest.sh
```

Release keystore (one-time, for stable updates):

```bash
bash scripts/create_release_keystore.sh
```

## Docs

| Doc | Purpose |
|---|---|
| `docs/INSTALL.md` | Step-by-step install for recipients |
| `docs/RELEASE.md` | CI/CD and APK naming |
| `docs/PRIVACY.md` | Privacy summary |
| `docs/PHYSICAL_TEST.md` | Device test matrix |
| `docs/DECISIONS.md` | Locked product decisions |

**Never use live bank OTPs in synthetic tests.**

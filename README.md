# Khidki

Personal Android app: forward OTP-shaped **SMS** to one trusted number while a window you start is open. Everything stays on your phone — no server, no internet permission.

**Not on Google Play.** Install from [GitHub Releases](https://github.com/laraib-sidd/khidki/releases) → **Latest** (`v1.4.0-b<N>` per `main` push).

Play Protect may warn on sideloaded SMS apps — see **`docs/PLAY_PROTECT.md`**.

## Install

See **`docs/INSTALL.md`** for the full guide. Short version:

1. Download `khidki-1.4.0-b*.apk` from GitHub Releases (Latest).
2. If upgrading from an old debug build (`dev.laraib.khidki.debug`), uninstall it first.
3. **App info → ⋮ → Allow restricted settings** (Android 15+), then grant SMS + notifications.
4. Realme / ColorOS: **Auto-start** on, battery **Unrestricted**.
5. Open Khidki → complete welcome → set trusted number → pick categories → **Start forwarding** on Home.

## How it works

| Step | What happens |
|---|---|
| **Rules** | Turn on message types (shopping OTPs, banks, government, etc.) |
| **Start forwarding** | Pick duration on Home — window opens immediately |
| **OTP SMS arrives** | If it matches an enabled category → forwarded to trusted number |
| **Stop** | Tap Stop on Home — window closes |

### SMS only — not RCS

Khidki reads **SMS**, not RCS chat. Bank and delivery OTPs that arrive as SMS work. Google Messages RCS chats do not.

## Use

1. **Settings** → trusted number (`+91…`).
2. **Rules** → toggle categories (Shopping, Banks, UPI, Government, …).
3. **Home** → **Start forwarding** → choose 15m–2h.
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

CI release signing: set GitHub secrets `KHIDKI_RELEASE_KEYSTORE_BASE64`, `KHIDKI_RELEASE_STORE_PASSWORD`, `KHIDKI_RELEASE_KEY_ALIAS`, `KHIDKI_RELEASE_KEY_PASSWORD`.

## Docs

| Doc | Purpose |
|---|---|
| `docs/INSTALL.md` | Step-by-step install for recipients |
| `docs/PLAY_PROTECT.md` | Play Protect behavior and appeal path |
| `docs/RELEASE.md` | CI/CD and APK naming |
| `docs/PRIVACY.md` | Privacy summary |
| `docs/PHYSICAL_TEST.md` | Device test matrix |
| `docs/DECISIONS.md` | Locked product decisions |

**Never use live bank OTPs in synthetic tests.**

# Khidki

<p align="center">
  <img src="docs/brand/khidki-mark.png" alt="Khidki logo" width="128" />
</p>

On-device Android app that forwards **SMS you choose** (including everything, or OTP categories) to people you configure, only while a window you start is open. No server. No `INTERNET` permission.

**License:** [GPL-3.0-or-later](LICENSE)  
**Not on Google Play.** Install from [GitHub Releases](https://github.com/laraib-sidd/khidki/releases) (owner-signed) or, once listed, [F-Droid](https://f-droid.org/) (F-Droid-signed). Those APKs **cannot** update each other in place — different signing keys. Pick one channel.

Play Protect may warn on **sideloaded** SMS apps — see [`docs/PLAY_PROTECT.md`](docs/PLAY_PROTECT.md). F-Droid installs are not sideloads.

## Install

See [`docs/INSTALL.md`](docs/INSTALL.md). Short version:

1. Download `khidki-1.0.2-*.apk` from GitHub Releases (**Latest**), or install from F-Droid when available.
2. If switching channel or upgrading from an older internal build or `dev.laraib.khidki.debug`, uninstall first.
3. **App info → ⋮ → Allow restricted settings** (Android 15+), then grant SMS + notifications.
4. Realme / ColorOS: **Auto-start** on, battery **Unrestricted**.
5. Open Khidki → welcome (first person) → **Rules** for more people → **Start forwarding** on Home (duration or Until I stop).

## How it works

| Step | What happens |
|---|---|
| **Rules** | Up to five people; each has their own filters (Shopping, Banks, UPI, Government, alerts, All SMS, named senders) |
| **Start forwarding** | Pick 15m–2h or **Until I stop** |
| **SMS arrives** | Classified once; sent to every enabled person whose filters match |
| **Stop** | Home or the ongoing notification shade |

### SMS only — not RCS

Khidki reads **SMS**, not RCS chat. Codes that arrive as SMS work. Google Messages RCS chats do not.

## Development

```bash
./gradlew :app:testDebugUnitTest :app:assembleRelease
bash scripts/audit_manifest.sh
```

Release keystore (one-time, for GitHub Release signing only — never commit it):

```bash
export KHIDKI_RELEASE_STORE_PASSWORD=...
export KHIDKI_RELEASE_KEY_PASSWORD=...
export KHIDKI_RELEASE_KEY_ALIAS=...
bash scripts/create_release_keystore.sh
```

CI secrets: `KHIDKI_RELEASE_KEYSTORE_BASE64`, `KHIDKI_RELEASE_STORE_PASSWORD`, `KHIDKI_RELEASE_KEY_ALIAS`, `KHIDKI_RELEASE_KEY_PASSWORD`.

## Docs

| Doc | Purpose |
|---|---|
| [`docs/INSTALL.md`](docs/INSTALL.md) | Install for recipients |
| [`docs/PLAY_PROTECT.md`](docs/PLAY_PROTECT.md) | Sideload Play Protect vs F-Droid |
| [`docs/RELEASE.md`](docs/RELEASE.md) | CI/CD, tags, two signing keys |
| [`docs/PRIVACY.md`](docs/PRIVACY.md) | Privacy summary |
| [`docs/fdroid/dev.laraib.khidki.yml`](docs/fdroid/dev.laraib.khidki.yml) | Draft F-Droid metadata |
| [`docs/DECISIONS.md`](docs/DECISIONS.md) | Locked product decisions |

**Never use live bank OTPs in synthetic tests.**

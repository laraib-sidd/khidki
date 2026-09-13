# Share-ready Khidki 1.0.0

**Project:** `~/pers/window-sms` (`laraib-sidd/khidki`)

## What / why

Public GitHub still looks like a private CI lab. First public release is **1.0.0**. Add a real logo. Tag-only signed Releases. Fill GitHub About. Delete stale tags/releases.

## Locked decisions

- `versionName = "1.0.0"`, `versionCode = 1` in `app/build.gradle.kts`. Tag `v1.0.0`. Internal 1.5.x discarded. Existing 1.5.1 installs uninstall first (new signing key; versionCode 1 is also a downgrade from 39).
- Keep only GitHub Release `v1.0.0`. Delete every older release and git tag (`v1.1.0-b28` … `v1.5.1`).
- Release job only on `v*` tags. Fail closed without `KHIDKI_RELEASE_*`. No silent debug-signed public APK. Local `assembleRelease` may still debug-sign.
- Play out. No INTERNET. F-Droid GitLab MR remains human; draft `docs/fdroid/dev.laraib.khidki.yml` → 1.0.0 / code 1 / commit `v1.0.0`.
- Do not rewrite historical `docs/plans/*` dumps.
- Logo this pass. Brand `#006A60` + `#F4FBF7`. Khidki = window. Geometric mark, readable at 48dp, no wordmark in adaptive-icon safe zone.
- Parent generates Canva PNGs. Implementer does not call Canva/Exa/browser. Wire files if present under `docs/brand/` and Fastlane/launcher paths.

## Signing — human (agent cannot set secrets)

`scripts/create_release_keystore.sh` must require env passwords (no hardcoded `khidki-release`).

1. Generate keystore with env passwords. Never commit it.
2. Backup `.keystore` forever.
3. GitHub Actions secrets: `KHIDKI_RELEASE_KEYSTORE_BASE64`, `KHIDKI_RELEASE_STORE_PASSWORD`, `KHIDKI_RELEASE_KEY_ALIAS`, `KHIDKI_RELEASE_KEY_PASSWORD`.
4. After `v1.0.0` CI: `apksigner verify --print-certs` must not be `CN=Android Debug`.

F-Droid remains a different key.

## Logo assets (parent / Canva)

- `fastlane/metadata/android/en-US/images/icon.png` — 512×512
- `docs/brand/khidki-mark.png` — source mark
- `docs/brand/github-social.png` — 1280×640
- Android PNG foreground wired in `mipmap-anydpi-v26/ic_launcher.xml`; keep `#006A60` background; safe-zone padding
- README logo at top

## Code / copy

**CI** (`.github/workflows/ci.yml`): `test`/`audit` on PR+main; `release` only `startsWith(github.ref, 'refs/tags/v')`; exit 1 if keystore secret empty; no `v*-bN` from `run_number`.

**Notes** (`scripts/generate_release_notes.sh`): FOSS 1.0.0, not “Personal APK”, not auto `-bN`.

**Docs:** README, INSTALL, RELEASE, PLAY_PROTECT, PRIVACY (drop `req`), CONTRIBUTING (no MODEL_MEMORY as required), CHANGELOG 1.0.0 first public, fastlane changelog `1.txt`, bug/PR templates (no preview/`req`/Realme GT 6T).

**GitHub About:** description = Fastlane short text; homepage = Releases URL; topics `android` `sms` `kotlin` `foss` `gplv3` `privacy`; `--enable-projects=false`.

**Ops (blocked on human secrets — do not tag until secrets exist):**

```bash
gh release delete <old-tag> --yes
git push origin :refs/tags/<old-tag>
git tag -a v1.0.0 -m "Khidki 1.0.0"
git push origin v1.0.0
```

## Files / symbols

- `.github/workflows/ci.yml`
- `app/build.gradle.kts`
- `scripts/create_release_keystore.sh`, `scripts/generate_release_notes.sh`
- README, CONTRIBUTING, CHANGELOG, docs/INSTALL.md, docs/RELEASE.md, docs/PLAY_PROTECT.md, docs/PRIVACY.md, docs/fdroid/dev.laraib.khidki.yml
- fastlane metadata, `.github` issue/PR templates
- launcher XML/PNG, `docs/brand/`

## Test command

```bash
./gradlew :app:testDebugUnitTest
bash scripts/audit_manifest.sh
```

## Done

- Public version 1.0.0 / 1
- Logo in launcher + Fastlane 512 + README
- About filled; tag-only signed Releases
- Keystore backup + four secrets (human); APK owner-signed after tag
- Stale releases/tags deleted (ops bead, after secrets)

## Out of scope

F-Droid submit, Play Protect appeal, reproducible dual-sign, deleting `docs/plans/` history, Room credential-table cleanup, pushing `v1.0.0` before secrets exist.

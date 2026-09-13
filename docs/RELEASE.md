# Releases

## Channels

| Channel | Who signs | Update path |
|---|---|---|
| **GitHub Releases** | Owner release keystore (`KHIDKI_RELEASE_KEYSTORE_*`) | Same key only |
| **F-Droid** | F-Droid buildserver key | Same F-Droid key only |

Package ID is `dev.laraib.khidki` on both. **Different certificates** — uninstall to switch. Reproducible dual-sign is not in this release.

## How GitHub releases work

| Item | Example |
|---|---|
| **Git tag** | `v1.0.0` |
| **APK name** | `khidki-1.0.0-b1-<sha>.apk` |
| **versionName / versionCode** | Baked in `app/build.gradle.kts` (`1.0.0` / `1`) |

- Each GitHub release is **immutable**
- CI publishes a signed release **only** when you push a `v*` tag
- F-Droid auto-update looks at **git tags** and the static `versionCode` / `versionName` in Gradle

Public release:

```bash
git tag -a v1.0.0 -m "Khidki 1.0.0"
git push origin v1.0.0
```

CI runs on the tag and publishes a release at that tag name. All four `KHIDKI_RELEASE_*` secrets must be set first or the job fails closed.

## CI

```
test --> assemble --> audit_manifest --> publish-github-release (tags only)
```

## Install (GitHub)

1. Open [GitHub Releases](https://github.com/laraib-sidd/khidki/releases) → **Latest**
2. Download `khidki-1.0.0-*.apk` (package `dev.laraib.khidki`)
3. See `docs/INSTALL.md`

## Secrets (GitHub Actions only — never commit)

| Secret | Purpose |
|---|---|
| `KHIDKI_RELEASE_KEYSTORE_BASE64` | Stable release keystore |
| `KHIDKI_RELEASE_STORE_PASSWORD` | Store password |
| `KHIDKI_RELEASE_KEY_ALIAS` | Key alias |
| `KHIDKI_RELEASE_KEY_PASSWORD` | Key password |

Generate locally:

```bash
export KHIDKI_RELEASE_STORE_PASSWORD=...
export KHIDKI_RELEASE_KEY_PASSWORD=...
export KHIDKI_RELEASE_KEY_ALIAS=...
bash scripts/create_release_keystore.sh
```

Keep `app/release-keystore/` gitignored.

## Local package (optional)

```bash
export KHIDKI_VERSION_NAME=1.0.0
bash scripts/prepare_release_apk.sh
ls -la release-artifacts/
```

Local `assembleRelease` may still fall back to the debug keystore when no release keystore is present.

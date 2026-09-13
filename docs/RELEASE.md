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
| **Git tag (CI on `main`)** | `v1.5.1-b<N>` |
| **Milestone tag** | `v1.5.1` |
| **APK name** | `khidki-1.5.1-b<N>-<sha>.apk` |
| **versionName / versionCode** | Baked in `app/build.gradle.kts` (`1.5.1` / `39`) |

- Each GitHub release is **immutable**
- F-Droid auto-update looks at **git tags** and the static `versionCode` / `versionName` in Gradle

Manual milestone:

```bash
git tag -a v1.5.1 -m "Khidki 1.5.1 FOSS listing"
git push origin v1.5.1
```

CI runs on the tag and publishes a release at that tag name.

## CI

```
test --> assemble --> audit_manifest --> publish-github-release
```

## Install (GitHub)

1. Open [GitHub Releases](https://github.com/laraib-sidd/khidki/releases) → **Latest**
2. Download `khidki-*-b*.apk` (package `dev.laraib.khidki`)
3. See `docs/INSTALL.md`

## Secrets (GitHub Actions only — never commit)

| Secret | Purpose |
|---|---|
| `KHIDKI_RELEASE_KEYSTORE_BASE64` | Stable release keystore |
| `KHIDKI_RELEASE_STORE_PASSWORD` | Store password |
| `KHIDKI_RELEASE_KEY_ALIAS` | Key alias |
| `KHIDKI_RELEASE_KEY_PASSWORD` | Key password |
| `KHIDKI_PREVIEW_KEYSTORE_BASE64` | Optional debug keystore override |

Generate locally: `bash scripts/create_release_keystore.sh`. Keep `/app/release-keystore/` gitignored.

## Local package (optional)

```bash
export KHIDKI_VERSION_NAME=1.5.1
bash scripts/prepare_release_apk.sh
ls -la release-artifacts/
```

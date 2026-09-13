#!/usr/bin/env bash
# Generate release notes body for GitHub Releases.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

RELEASE_TAG="${KHIDKI_RELEASE_TAG:-${GITHUB_REF_NAME:-v1.0.0}}"
VERSION_NAME="${KHIDKI_VERSION_NAME:-unknown}"
VERSION_CODE="${KHIDKI_VERSION_CODE:-0}"
GIT_SHA="${GITHUB_SHA:-$(git rev-parse HEAD)}"
SHORT_SHA="${GIT_SHA:0:7}"
BUILD_TIME="$(date -u +"%Y-%m-%d %H:%M UTC")"
RUN_URL="${GITHUB_SERVER_URL:-https://github.com}/${GITHUB_REPOSITORY:-local}/actions/runs/${GITHUB_RUN_ID:-0}"
APK_BASENAME="${APK_BASENAME:-khidki-${VERSION_NAME}-b${VERSION_CODE}-${SHORT_SHA}.apk}"

COMMIT_MSG="$(git log -1 --pretty=format:%s 2>/dev/null || echo 'local build')"
COMMIT_AUTHOR="$(git log -1 --pretty=format:%an 2>/dev/null || echo 'unknown')"

GITHUB_REPOSITORY="${GITHUB_REPOSITORY:-laraib-sidd/khidki}"
GITHUB_SERVER_URL="${GITHUB_SERVER_URL:-https://github.com}"
GITHUB_RUN_NUMBER="${GITHUB_RUN_NUMBER:-0}"
GITHUB_RUN_ID="${GITHUB_RUN_ID:-0}"

cat <<EOF
## Khidki ${VERSION_NAME}

First public FOSS release (${RELEASE_TAG}).

| Field | Value |
|---|---|
| Version | \`${VERSION_NAME}\` (code **${VERSION_CODE}**) |
| Release tag | \`${RELEASE_TAG}\` |
| Commit | [\`${SHORT_SHA}\`](${GITHUB_SERVER_URL:-https://github.com}/${GITHUB_REPOSITORY}/commit/${GIT_SHA}) |
| Built | ${BUILD_TIME} |
| Workflow | [run #${GITHUB_RUN_NUMBER:-0}](${RUN_URL}) |
| Commit message | ${COMMIT_MSG} |
| Author | ${COMMIT_AUTHOR} |

### Install
1. Download **${APK_BASENAME}** (and optional \`.sha256\` checksum).
2. Uninstall any older Khidki build if Android blocks the update (new signing key or lower version code).
3. Android 15+: App Info → Allow restricted settings → Grant SMS and notifications.
4. See [\`docs/INSTALL.md\`](https://github.com/${GITHUB_REPOSITORY:-laraib-sidd/khidki}/blob/main/docs/INSTALL.md) for full steps.

### Verify checksum
\`\`\`bash
sha256sum -c ${APK_BASENAME}.sha256
\`\`\`

> GPL-3.0-or-later FOSS. Not on Google Play. No INTERNET permission.
EOF

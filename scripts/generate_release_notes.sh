#!/usr/bin/env bash
# Generate release notes body for GitHub Releases.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

CHANNEL="${KHIDKI_RELEASE_CHANNEL:-preview}"
RELEASE_TAG="${KHIDKI_RELEASE_TAG:-preview}"
VERSION_NAME="${KHIDKI_VERSION_NAME:-unknown}"
VERSION_CODE="${KHIDKI_VERSION_CODE:-0}"
GIT_SHA="${GITHUB_SHA:-$(git rev-parse HEAD)}"
SHORT_SHA="${GIT_SHA:0:7}"
BUILD_TIME="$(date -u +"%Y-%m-%d %H:%M UTC")"
RUN_URL="${GITHUB_SERVER_URL:-https://github.com}/${GITHUB_REPOSITORY:-local}/actions/runs/${GITHUB_RUN_ID:-0}"
APK_BASENAME="${APK_BASENAME:-khidki-${VERSION_NAME}-b${VERSION_CODE}-${SHORT_SHA}.apk}"

COMMIT_MSG="$(git log -1 --pretty=format:%s 2>/dev/null || echo 'local build')"
COMMIT_AUTHOR="$(git log -1 --pretty=format:%an 2>/dev/null || echo 'unknown')"

cat <<EOF
## Khidki ${VERSION_NAME} (${CHANNEL})

| Field | Value |
|---|---|
| Version | \`${VERSION_NAME}\` (code **${VERSION_CODE}**) |
| Release tag | \`${RELEASE_TAG}\` |
| Commit | [\`${SHORT_SHA}\`](${GITHUB_SERVER_URL:-https://github.com}/${GITHUB_REPOSITORY}/commit/${GIT_SHA}) |
| Built | ${BUILD_TIME} |
| Workflow | [run #${GITHUB_RUN_NUMBER:-0}](${RUN_URL}) |
| Commit message | ${COMMIT_MSG} |
| Author | ${COMMIT_AUTHOR} |

### Install (sideload)
1. Download **${APK_BASENAME}** (and optional \`.sha256\` checksum).
2. Install over an existing Khidki build — same signing key, higher version code required.
3. Android 15/16: App Info → Allow restricted settings → Grant SMS.

### Verify checksum
\`\`\`bash
sha256sum -c ${APK_BASENAME}.sha256
\`\`\`

### Tags
- **\`v<version>-b<N>\`** (e.g. \`v1.1.0-b17\`): auto-created on every push to \`main\` after CI passes.
- **\`v*\` manual tags**: push a git tag for milestone snapshots (e.g. \`v1.2.0\`).

> Personal APK. Not on Play Store. No INTERNET permission.
EOF

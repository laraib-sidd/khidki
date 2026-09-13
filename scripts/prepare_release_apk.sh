#!/usr/bin/env bash
# Package the debug APK with a traceable filename and checksum for GitHub Releases.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

VERSION_NAME="${KHIDKI_VERSION_NAME:-unknown}"
VERSION_CODE="${KHIDKI_VERSION_CODE:-0}"
GIT_SHA="${GITHUB_SHA:-$(git rev-parse HEAD 2>/dev/null || echo local)}"
SHORT_SHA="${GIT_SHA:0:7}"
BUILD_TIME="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
RUN_ID="${GITHUB_RUN_ID:-local}"
RUN_NUMBER="${GITHUB_RUN_NUMBER:-0}"
RELEASE_TAG="${KHIDKI_RELEASE_TAG:-${KHIDKI_RELEASE_CHANNEL:-preview}}"

SRC_APK="$(find app/build/outputs/apk/release -name '*.apk' -type f | sort | head -1)"
if [[ -z "$SRC_APK" ]]; then
  echo "ERROR: No release APK found under app/build/outputs/apk/release" >&2
  exit 1
fi

OUT_DIR="$ROOT/release-artifacts"
mkdir -p "$OUT_DIR"

APK_BASENAME="khidki-${VERSION_NAME}-b${VERSION_CODE}-${SHORT_SHA}.apk"
OUT_APK="$OUT_DIR/$APK_BASENAME"
cp "$SRC_APK" "$OUT_APK"

(
  cd "$OUT_DIR"
  sha256sum "$APK_BASENAME" > "${APK_BASENAME}.sha256"
)

cat > "$OUT_DIR/release-metadata.json" <<EOF
{
  "versionName": "${VERSION_NAME}",
  "versionCode": ${VERSION_CODE},
  "gitSha": "${GIT_SHA}",
  "shortSha": "${SHORT_SHA}",
  "buildTimeUtc": "${BUILD_TIME}",
  "workflowRunId": "${RUN_ID}",
  "workflowRunNumber": ${RUN_NUMBER},
  "apkFile": "${APK_BASENAME}",
  "channel": "${KHIDKI_RELEASE_CHANNEL:-preview}",
  "releaseTag": "${RELEASE_TAG}"
}
EOF

echo "Prepared release artifact: $OUT_APK"
if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
  echo "apk_basename=$APK_BASENAME" >> "$GITHUB_OUTPUT"
fi

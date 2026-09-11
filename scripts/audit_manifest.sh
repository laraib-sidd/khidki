#!/usr/bin/env bash
# Fail CI if android.permission.INTERNET appears in Khidki manifests.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SOURCE_MANIFEST="$ROOT/app/src/main/AndroidManifest.xml"

if grep -q 'android.permission.INTERNET' "$SOURCE_MANIFEST"; then
  echo "FAIL: INTERNET permission found in source manifest: $SOURCE_MANIFEST"
  exit 1
fi

MERGED_CANDIDATES=(
  "$ROOT/app/build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml"
  "$ROOT/app/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml"
  "$ROOT/app/build/intermediates/packaged_manifests/debug/processDebugManifestForPackage/AndroidManifest.xml"
)

MERGED_MANIFEST=""
for candidate in "${MERGED_CANDIDATES[@]}"; do
  if [[ -f "$candidate" ]]; then
    MERGED_MANIFEST="$candidate"
    break
  fi
done

if [[ -z "$MERGED_MANIFEST" ]]; then
  echo "FAIL: No merged debug manifest found. Run ./gradlew :app:assembleDebug first."
  exit 1
fi

if grep -q 'android.permission.INTERNET' "$MERGED_MANIFEST"; then
  echo "FAIL: INTERNET permission found in merged manifest: $MERGED_MANIFEST"
  exit 1
fi

echo "PASS: Source and merged manifests contain no INTERNET permission."
echo "Checked: $SOURCE_MANIFEST"
echo "Checked: $MERGED_MANIFEST"

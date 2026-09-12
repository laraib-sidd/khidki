#!/usr/bin/env bash
# Create a release keystore for Khidki sideload distribution.
# Run once locally; never commit the keystore (gitignored).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
KEYSTORE_DIR="$ROOT/app/release-keystore"
KEYSTORE_FILE="$KEYSTORE_DIR/release.keystore"

mkdir -p "$KEYSTORE_DIR"

if [[ -f "$KEYSTORE_FILE" ]]; then
  echo "Keystore already exists: $KEYSTORE_FILE"
  exit 0
fi

keytool -genkeypair \
  -v \
  -keystore "$KEYSTORE_FILE" \
  -alias khidki \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass khidki-release \
  -keypass khidki-release \
  -dname "CN=Khidki, OU=Personal, O=Laraib, L=India, C=IN"

echo "Created $KEYSTORE_FILE"
echo "For CI, base64-encode and set GitHub secret KHIDKI_RELEASE_KEYSTORE_BASE64"

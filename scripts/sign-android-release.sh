#!/usr/bin/env bash
set -euo pipefail
: "${ANDROID_HOME:?Android SDK is required}"
: "${ANDROID_KEYSTORE_PATH:?A private Release keystore is required}"
: "${ANDROID_KEYSTORE_PASSWORD:?Keystore password is required}"
: "${ANDROID_KEY_ALIAS:?Key alias is required}"
: "${ANDROID_KEY_PASSWORD:?Key password is required}"
INPUT_APK=${1:?Usage: sign-android-release.sh unsigned.apk output.apk}
OUTPUT_APK=${2:?Output path is required}
BUILD_TOOLS="$ANDROID_HOME/build-tools/35.0.0"
TASK_TEMP=$(mktemp -d)
trap 'rm -rf "$TASK_TEMP"' EXIT
"$BUILD_TOOLS/zipalign" -P 16 -f 4 "$INPUT_APK" "$TASK_TEMP/aligned.apk"
"$BUILD_TOOLS/apksigner" sign --ks "$ANDROID_KEYSTORE_PATH" --ks-key-alias "$ANDROID_KEY_ALIAS" \
 --ks-pass env:ANDROID_KEYSTORE_PASSWORD --key-pass env:ANDROID_KEY_PASSWORD \
 --out "$OUTPUT_APK" "$TASK_TEMP/aligned.apk"
"$BUILD_TOOLS/apksigner" verify --verbose --print-certs "$OUTPUT_APK"
"$BUILD_TOOLS/aapt" dump badging "$OUTPUT_APK" > "$TASK_TEMP/badging.txt"
grep -F "package: name='com.moataz.motazalqmi'" "$TASK_TEMP/badging.txt"
grep -F "sdkVersion:'26'" "$TASK_TEMP/badging.txt"
if unzip -Z1 "$OUTPUT_APK" | grep -Eq 'lib/(armeabi|armeabi-v7a|x86|x86_64)/'; then
 echo 'Unexpected production ABI' >&2; exit 1
fi
sha256sum "$OUTPUT_APK"

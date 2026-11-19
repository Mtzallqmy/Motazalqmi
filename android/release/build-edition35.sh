#!/usr/bin/env bash
set -euo pipefail
umask 077

package=com.moataz.motazalqmi.edition35
sdk="$ANDROID_HOME/build-tools/35.0.0"
signing_dir=$(mktemp -d "$RUNNER_TEMP/edition35.XXXXXX")
trap 'rm -rf "$signing_dir"' EXIT
mkdir -p dist

signing=unsigned
asset='Motazalqmi-3.5.0-ARM64.apk'
if [[ -n "$KEYSTORE64" && -n "$ANDROID_KEYSTORE_PASSWORD" && -n "$ANDROID_KEY_ALIAS" && -n "$ANDROID_KEY_PASSWORD" ]]; then
  export ANDROID_KEYSTORE_PATH="$signing_dir/edition35.p12"
  printf '%s' "$KEYSTORE64" | base64 -d > "$ANDROID_KEYSTORE_PATH"
  signing=signed
elif [[ -n "$KEYSTORE64$ANDROID_KEYSTORE_PASSWORD$ANDROID_KEY_ALIAS$ANDROID_KEY_PASSWORD" ]]; then
  echo '::error::Edition 35 signing configuration is incomplete.'
  exit 1
fi
unset KEYSTORE64

"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" 'platforms;android-35' 'build-tools;35.0.0'
bash android/gradlew -p android :app:testDebugUnitTest :app:lintDebug :app:assembleRelease -PmotazEdition35=true --no-daemon
if [[ "$signing" == unsigned ]]; then
  echo '::notice::Restore the Edition 35 signing key to publish this APK.'
  echo 'signed=false' >> "$GITHUB_OUTPUT"
  exit 0
fi

apk="dist/$asset"
cp android/app/build/outputs/apk/release/app-release.apk "$apk"
"$sdk/apksigner" verify --min-sdk-version 26 --verbose --print-certs "$apk" > dist/signature.txt
certificate=$(awk '/Signer #1 certificate SHA-256 digest:/ {print $NF}' dist/signature.txt)
test "$certificate" = "$(cat android/release/edition35-certificate.sha256)"
"$sdk/zipalign" -c 4 "$apk"
"$sdk/aapt" dump badging "$apk" > "$signing_dir/badging"
grep -F "package: name='$package' versionCode='30500' versionName='3.5.0'" "$signing_dir/badging"
grep -Fx "sdkVersion:'26'" "$signing_dir/badging"
grep -Fx "native-code: 'arm64-v8a'" "$signing_dir/badging"
grep -Fx "application-label:'معتز العلقمي'" "$signing_dir/badging"
unzip -tq "$apk"
printf '%s\n' "$package" > dist/package.txt
(cd dist && sha256sum "$asset" > SHA256SUMS)

echo 'signed=true' >> "$GITHUB_OUTPUT"

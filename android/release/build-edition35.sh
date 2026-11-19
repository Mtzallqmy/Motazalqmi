#!/usr/bin/env bash
set -euo pipefail
umask 077

package=com.moataz.motazalqmi.edition35
baseline=v3.5.0-preview.1
sdk="$ANDROID_HOME/build-tools/35.0.0"
signing_dir=$(mktemp -d "$RUNNER_TEMP/edition35.XXXXXX")
trap 'rm -rf "$signing_dir"' EXIT
mkdir -p dist

signing=unsigned
asset='Motazalqmi-3.5.0-ARM64.apk'
release_url="repos/$GITHUB_REPOSITORY/releases/tags/$baseline"
if gh api "$release_url" > "$signing_dir/baseline.json" 2> "$signing_dir/lookup-error"; then
  baseline_exists=true
elif grep -Fq '(HTTP 404)' "$signing_dir/lookup-error"; then
  baseline_exists=false
else
  cat "$signing_dir/lookup-error" >&2
  exit 1
fi

if [[ -n "$KEYSTORE64" && -n "$ANDROID_KEYSTORE_PASSWORD" && -n "$ANDROID_KEY_ALIAS" && -n "$ANDROID_KEY_PASSWORD" ]]; then
  test "$baseline_exists" = true
  export ANDROID_KEYSTORE_PATH="$signing_dir/edition35.p12"
  printf '%s' "$KEYSTORE64" | base64 -d > "$ANDROID_KEYSTORE_PATH"
  signing=existing
elif [[ -n "$KEYSTORE64$ANDROID_KEYSTORE_PASSWORD$ANDROID_KEY_ALIAS$ANDROID_KEY_PASSWORD" ]]; then
  echo '::error::Edition 35 signing configuration is incomplete.'
  exit 1
elif [[ "$GITHUB_REF" == refs/heads/main && "$baseline_exists" == false ]]; then
  grep -Eq "^[[:space:]]+versionName '3\.5\.0'$" android/app/build.gradle
  export ANDROID_KEYSTORE_PATH="$signing_dir/edition35.p12"
  ANDROID_KEYSTORE_PASSWORD=$(openssl rand -hex 32)
  echo "::add-mask::$ANDROID_KEYSTORE_PASSWORD"
  export ANDROID_KEYSTORE_PASSWORD
  export ANDROID_KEY_PASSWORD="$ANDROID_KEYSTORE_PASSWORD"
  export ANDROID_KEY_ALIAS=motazalqmi-edition35
  keytool -genkeypair -keystore "$ANDROID_KEYSTORE_PATH" -storetype PKCS12 \
    -storepass:env ANDROID_KEYSTORE_PASSWORD -keypass:env ANDROID_KEY_PASSWORD \
    -alias "$ANDROID_KEY_ALIAS" -keyalg RSA -keysize 3072 -sigalg SHA256withRSA \
    -validity 10950 -dname 'CN=Motaz Alalqami, O=Motaz Alalqami, C=YE'
  signing=bootstrap
fi
unset KEYSTORE64

"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" 'platforms;android-35' 'build-tools;35.0.0'
if [[ "$signing" == existing ]]; then
  mkdir "$signing_dir/baseline"
  gh release download "$baseline" -p "$asset" -D "$signing_dir/baseline"
  baseline_apk="$signing_dir/baseline/$asset"
  test "$("$sdk/aapt2" dump packagename "$baseline_apk")" = "$package"
  "$sdk/apksigner" verify --min-sdk-version 26 --print-certs "$baseline_apk" > "$signing_dir/baseline-cert.txt"
  baseline_certificate=$(awk '/Signer #1 certificate SHA-256 digest:/ {print $NF}' "$signing_dir/baseline-cert.txt")
fi

bash android/gradlew -p android :app:testDebugUnitTest :app:lintDebug :app:assembleRelease -PmotazEdition35=true --no-daemon
if [[ "$signing" == unsigned ]]; then
  echo 'signed=false' >> "$GITHUB_OUTPUT"
  exit 0
fi

apk="dist/$asset"
cp android/app/build/outputs/apk/release/app-release.apk "$apk"
"$sdk/apksigner" verify --min-sdk-version 26 --verbose --print-certs "$apk" > dist/signature.txt
certificate=$(awk '/Signer #1 certificate SHA-256 digest:/ {print $NF}' dist/signature.txt)
test "${#certificate}" = 64
if [[ "$signing" == existing ]]; then
  test "$certificate" = "$baseline_certificate"
fi
"$sdk/zipalign" -c 4 "$apk"
"$sdk/aapt" dump badging "$apk" > "$signing_dir/badging"
grep -F "package: name='$package' versionCode='30500' versionName='3.5.0'" "$signing_dir/badging"
grep -Fx "sdkVersion:'26'" "$signing_dir/badging"
grep -Fx "native-code: 'arm64-v8a'" "$signing_dir/badging"
grep -Fx "application-label:'معتز العلقمي'" "$signing_dir/badging"
unzip -tq "$apk"
printf '%s\n' "$package" > dist/package.txt
(cd dist && sha256sum "$asset" > SHA256SUMS)

if [[ "$signing" == bootstrap ]]; then
  printf '%s\n' "$ANDROID_KEY_ALIAS" > "$signing_dir/alias.txt"
  printf '%s' "$ANDROID_KEYSTORE_PASSWORD" > "$signing_dir/password.txt"
  tar -C "$signing_dir" -czf - edition35.p12 alias.txt password.txt |
    openssl cms -encrypt -binary -aes-256-gcm -outform DER -out "$RUNNER_TEMP/edition35-signing-backup.cms" \
      android/release/edition35-recovery-certificate.pem
fi
echo 'signed=true' >> "$GITHUB_OUTPUT"

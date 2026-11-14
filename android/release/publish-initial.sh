#!/usr/bin/env bash
set -euo pipefail
umask 077
release_tag=v2.0.0
current_head="$(gh api "repos/$GITHUB_REPOSITORY/git/ref/heads/$GITHUB_REF_NAME" --jq .object.sha)"
if [ "$current_head" != "$GITHUB_SHA" ]; then echo 'Newer source exists; refusing stale publication.'; exit 1; fi
output="$GITHUB_WORKSPACE/android/release/output"
private_dir="$RUNNER_TEMP/motaz-signing"
mkdir -p "$output" "$private_dir"
# Reserve the release before generating a key, so retries never rotate it silently.
gh release create "$release_tag" --repo "$GITHUB_REPOSITORY" --target "$GITHUB_SHA" --draft --title 'مدونة معتز العلقمي — 2.0.0' --notes-file android/release/RELEASE_NOTES.md
export ANDROID_KEYSTORE_PATH="$private_dir/release.jks"
if [ -n "${KEYSTORE_BASE64:-}" ]; then
  printf '%s' "$KEYSTORE_BASE64" | base64 --decode > "$ANDROID_KEYSTORE_PATH"
  : "${ANDROID_KEYSTORE_PASSWORD:?}" "${ANDROID_KEY_PASSWORD:?}" "${ANDROID_KEY_ALIAS:?}"
else
  export ANDROID_KEYSTORE_PASSWORD="$(openssl rand -hex 32)"
  export ANDROID_KEY_PASSWORD="$ANDROID_KEYSTORE_PASSWORD"
  export ANDROID_KEY_ALIAS=motazalqmi-production
  echo "::add-mask::$ANDROID_KEYSTORE_PASSWORD"
  keytool -genkeypair -keystore "$ANDROID_KEYSTORE_PATH" -storetype PKCS12 -alias "$ANDROID_KEY_ALIAS" -storepass:env ANDROID_KEYSTORE_PASSWORD -keypass:env ANDROID_KEY_PASSWORD -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=Motaz Alalqami, O=Motaz Alalqami, L=Taiz, C=YE'
fi
printf '%s' "$ANDROID_KEYSTORE_PASSWORD" > "$private_dir/password.txt"
printf '%s' "$ANDROID_KEY_PASSWORD" > "$private_dir/key-password.txt"
printf '%s' "$ANDROID_KEY_ALIAS" > "$private_dir/alias.txt"
tar -C "$private_dir" -cf "$private_dir/signing.tar" release.jks password.txt key-password.txt alias.txt
openssl cms -encrypt -aes-256-cbc -binary -in "$private_dir/signing.tar" -out "$output/signing-backup.cms" -outform DER android/release/recovery-certificate.pem
# Only encrypted recovery leaves this directory. The private recovery key is held by the owner.
gh release upload "$release_tag" "$output/signing-backup.cms" --repo "$GITHUB_REPOSITORY"
sdkmanager 'platforms;android-35' 'build-tools;35.0.0'
gradle -p android :app:assembleRelease --no-daemon
apk=android/app/build/outputs/apk/release/app-release.apk
bash scripts/verify-apk.sh "$apk" | tee "$output/signature.txt"
if grep -q 'CN=Android Debug' "$output/signature.txt"; then exit 1; fi
cp "$apk" "$output/Motazalqmi-v2.0.0-release.apk"
(cd "$output" && sha256sum Motazalqmi-v2.0.0-release.apk > SHA256SUMS)
gh release upload "$release_tag" "$output/Motazalqmi-v2.0.0-release.apk" "$output/signature.txt" "$output/SHA256SUMS" --repo "$GITHUB_REPOSITORY"
gh release edit "$release_tag" --repo "$GITHUB_REPOSITORY" --draft=false
rm -rf "$private_dir"

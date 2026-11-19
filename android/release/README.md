# Production signing

The old successful Android workflow skipped its Release steps: no persistent signing secrets were configured. The previous downloadable APK was Debug. A new production certificate cannot update that installation; do not uninstall an old copy before securing its data.

`premium-release.yml` runs build, unit, lint and API 26/29 UI checks before publishing. `initialize-v2` enables the one-time v2.0.0 release after validation. If a v2.0.0 release already exists, it refuses to generate another identity. A reserved draft prevents silent rotation after failures.

If the four ANDROID signing secrets exist, they are reused. Otherwise the first release generates a random-password RSA-3072 production key. Only a CMS-encrypted backup leaves the runner; its RSA recovery private key is held privately by the owner outside GitHub. The public recovery certificate is safe to commit.

To restore, decrypt `signing-backup.cms` with the owner's recovery key. The archive contains release.jks, password.txt, key-password.txt and alias.txt. Configure ANDROID_KEYSTORE_BASE64, ANDROID_KEYSTORE_PASSWORD, ANDROID_KEY_PASSWORD and ANDROID_KEY_ALIAS as repository secrets for all future versions. Never create a replacement key for an update.

A successful `verify-apk.sh` prints package information, certificate and signature schemes, and rejects native libraries outside ARM64. Kotlin/Java-only packages do not need artificial native code. API 26/29 emulator testing uses Debug with x86_64 support; Release keeps arm64-v8a only.

## Edition 35

`v3.5.0-preview.1` starts the `com.moataz.motazalqmi.edition35` installation line. It installs alongside 3.4.1. All later updates for this line must keep the package ID, raise the version code and match the SHA-256 certificate digest in `edition35-certificate.sha256`.

Restore the owner’s encrypted recovery kit to configure these GitHub Actions secrets:

- `ANDROID_EDITION35_KEYSTORE_BASE64`
- `ANDROID_EDITION35_KEYSTORE_PASSWORD`
- `ANDROID_EDITION35_KEY_ALIAS`
- `ANDROID_EDITION35_KEY_PASSWORD`

Build with `-PmotazEdition35=true`. When preparing the next release, update the expected version code, APK asset name and release tag in the release workflow and build script. The workflow builds unsigned when the secrets are absent and checks the pinned certificate before publishing a signed APK. Edition 35 has its own signing identity and its own data directory.

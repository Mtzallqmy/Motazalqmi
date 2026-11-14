# Imported Premium v2 source — safe reconciliation

The owner-uploaded archive `motazalqmi-android-premium-v2.zip` was inspected and preserved as **60 textual source files** under `imports/premium-v2-upload/`. Archive SHA-256: `cc768c22bcca4011fefe8dcd03db719d7c75c600949522ca1c7d2610d1de26bd`.

## Why it is not copied over production

The uploaded API expects separate PostgreSQL tables named `articles`, `categories`, and `quotes`, and returns a `{ok,data}` envelope. Our deployed `Neon` project currently contains `blog_articles`, `blog_public_metadata`, and `blog_login_attempts`, and its Android app expects the authoritative `/api/v1/sync` snapshot. Overwriting the current API or Android app with the uploaded files would cause real sync failures.

The uploaded archive also lacks the Gradle Wrapper (`gradlew` plus wrapper JAR), and its signed-release workflow assumes signing secrets which may not be present. The existing application and production API remain the release source of truth.

## Integrated changes

- Retained all uploaded source without overwriting production.
- Ported additional Arabic orthographic normalization into the active Android reader search, with unit tests.
- Tightened the active GitHub Actions workflow to verify APK signature, package ID, and API 26 minimum before publishing a stand-alone debug artifact.
- Preserved the existing signed GitHub Release `v2.0.0`; do not overwrite it or rotate its release signing key.
- Verified Neon schema read-only; no destructive or speculative migration is needed.

Active app: `android/`. Reference-only imported app: `imports/premium-v2-upload/android/`.

Further porting from the uploaded variant must be accompanied by a mapping to the public snapshot API, Android tests and a working CI build.

# Motaz Alalqami: One repository, two delivery targets

**Canonical source:** https://github.com/Mtzallqmy/Motazalqmi at `main`.

- **Vercel production website:** `motazalqmi.vercel.app` builds the React/Vite website and serverless APIs from `main`.
- **Android:** `.github/workflows/edition-release.yml` builds native Android code under `android/` from `main` after web, unit, lint, API 26/28/29 UI and exact signed ARM64 installation/launch checks. Older standalone workflows remain for historical releases.
- **Native editorial UX:** `android/app/src/main/java/com/moataz/motazalqmi/ui/EditorialExperience.kt` provides a new Arabic magazine-style home, category browsing, quote, featured-story and article cards. The app is not a WebView wrapper.
- **Data:** the website and Android client both use the existing API backed by Neon; Vercel Blob remains the media store. Publishing new articles must happen through the authenticated CMS, not by inventing content in a release.
- **Offline:** Room and the bundled seed remain accessible without an internet connection; online changes sync through the existing API.
- **APK identity:** official `com.moataz.motazalqmi` requires its original signing certificate to update an installed app; the clearly marked independent 3.3 release uses `com.moataz.motazalqmi.edition33` alongside older installations, whose local saved data stays intact.
- **Do not replace older release tags:** each release uses its own tag and filename; all assets must match their built commit.

## Release 3.2

The 3.2 release pipeline is edition-release.yml. It builds main, runs web/Android verification and publishes an APK with SHA256SUMS. A signing-compatible production APK uses the original certificate. The independent Release variant uses com.moataz.motazalqmi.edition3. See RELEASE-3.2.md for verification scope and installation details.

## Release 3.3

The current native editorial studio adds author profiles, media management and scheduled publication. The website and native writer use the same APIs. Migration 004 is additive and was applied to the existing Neon main project. See RELEASE-3.3.md and EDITORIAL-3.3-MIGRATION.md for contracts, verification and operational requirements. Telegram activation requires a rotated token in production configuration and a successful account pairing test.

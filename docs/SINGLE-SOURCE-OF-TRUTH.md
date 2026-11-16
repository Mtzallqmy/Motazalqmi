# Motaz Alalqami: One repository, two delivery targets

**Canonical source:** https://github.com/Mtzallqmy/Motazalqmi at `main`.

- **Vercel production website:** `motazalqmi.vercel.app` builds the React/Vite website and serverless APIs from `main`.
- **Android:** `.github/workflows/android-standalone.yml` builds native Android code under `android/` from `main` and publishes `v3.1.0-standalone.1` only after ARM64 / Android API 26, 28 validation succeeds.
- **Native editorial UX:** `android/app/src/main/java/com/moataz/motazalqmi/ui/EditorialExperience.kt` provides a new Arabic magazine-style home, category browsing, quote, featured-story and article cards. The app is not a WebView wrapper.
- **Data:** the website and Android client both use the existing API backed by Neon; Vercel Blob remains the media store. Publishing new articles must happen through the authenticated CMS, not by inventing content in a release.
- **Offline:** Room and the bundled seed remain accessible without an internet connection; online changes sync through the existing API.
- **APK identity:** official `com.moataz.motazalqmi` requires its original signing certificate to update an installed app; the clearly marked separate test app `com.moataz.motazalqmi.preview31` installs alongside without touching production user data.
- **Do not replace older release tags:** the 3.1 preview uses a unique tag and filename; all assets must match their built commit.

## Release 3.2

The current release pipeline is edition-release.yml. It builds main, runs web/Android verification and publishes an APK with SHA256SUMS. A signing-compatible production APK uses the original certificate. The independent Release variant uses com.moataz.motazalqmi.edition3. See RELEASE-3.2.md for verification scope and installation details.

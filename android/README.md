# مدونة معتز العلقمي — Android 2.0

برمجة وتطوير: معتز العلقمي — تعز — اليمن — 2026م.

Native Kotlin/Compose reader; no WebView. Application ID `com.moataz.motazalqmi`, minSdk 26, target/compile 35, version 2.0.0/code 20000. Production ABI arm64-v8a. DataStore/Compose supply genuine native libraries; the old dummy identity library is removed.

## Build

Java 17 + Gradle 8.9 + Android SDK platform 35 are required.

```sh
gradle -p android :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
gradle -p android :app:assembleRelease
```

Release is unsigned unless all four environment variables are configured: `ANDROID_KEYSTORE_PATH`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`. CI reads corresponding protected GitHub signing secrets, with `ANDROID_KEYSTORE_BASE64` supplying the file. Never commit signing material.

Run emulator tests using `gradle -p android connectedDebugAndroidTest -PtestAbi=x86_64`. This changes only a test build's ABI; do not distribute it as ARM64.

## Source organization

- `data`: Retrofit, published snapshots, Room entities/DAO, preferences, atomic migration and repository.
- `domain`: Arabic search normalization shared across local search fields.
- `ui`: ViewModel, Compose screens, typography, Markdown engine, reader controls.
- `BlogApplication`: Hilt graph and constrained WorkManager job.

Room is the source read by UI Flow. Public content and user reading state live in separate tables. All snapshot rows are validated before a transaction retires old published rows, upserts new rows, updates metadata, and prunes unsaved retired rows. Saved retired content is retained privately and marked unavailable. Missing/incomplete/error responses never authorize deletion. A complete empty snapshot does.

First launch imports the old JSON cache and SharedPreferences `saved` IDs without deleting the originals. New installs use the published website snapshot whose provenance is packaged in assets. Last network sync is kept separately from seed installation.

Sync runs at launch, on explicit request, and periodically (six-hour minimum requested interval) under WorkManager network/battery constraints with exponential retry. Scheduling is subject to Android restrictions; no instant background delivery is promised.

Reader preferences use DataStore; positions/bookmarks/history use Room. Three redistributable SIL OFL Arabic fonts are bundled and licenses are in assets and `android/licenses`.

## Known release considerations

A newly generated production certificate cannot update an older Debug or differently signed installation. Preserve the new certificate for subsequent releases. Hardware ARM64 performance must be measured on actual devices; x86 emulator tests alone do not establish it.

The new API endpoints must be deployed before testing network refresh. The published snapshot currently has a fail-closed limit of 2000 articles / 3 MiB to stay within serverless response bounds; larger catalogues need versioned snapshot paging.

# Premium v2 engineering audit

Owner: معتز العلقمي — تعز — اليمن — 2026م

Base: 7969b3202cf59dff88a7094988e3f2cd82874e58, inspected 2026-11-14.
Branch: feat/motazalqami-android-premium-v2.

## Verified baseline
- Native Java Activities, hand-built layouts; no WebView. No Kotlin, Compose, Room, Hilt, DataStore or WorkManager.
- Single JSON content cache; SharedPreferences `motazalqmi-reader-v1` / `saved` stores bookmarks. Preserve both during migration.
- ARM64 JNI library returns an ABI string only; remove it rather than ship a redundant native binary.
- API v1 reads published Neon articles through server-only DATABASE_URL. Shared sections/quotes are the website's editorial metadata (not Neon tables).
- Public bootstrap fetched successfully: seven published articles, eight sections, four quotes. Seed offline from this actual response, never generate filler.
- Web admin uses signed HttpOnly sessions, scrypt and origin checks. Reader will not embed admin credentials or add an unprotected writer mode.
- Existing CI builds Debug; Release is conditional on four signing secrets. No GitHub Releases/tags found at audit time. No keystore in tracked files.
- Existing API tests: five passed. No Android tests or Gradle wrapper supplied.

## Problems to address
- No persisted reading position/history, reader typography preferences or background sync.
- Partial Markdown renderer and no accessible Compose design system.
- Cache replacement is not a database transaction and pagination can mix revisions during concurrent edits.
- No authoritative revision/deletion protocol. Need a published-set manifest and transactionally applied snapshot.
- Search lacks Arabic normalization, author and tag matching.
- No release signature verification or release publication pipeline.
- No empirical device performance evidence; do not claim physical ARM64 testing from a successful build.

## Constraints
Keep package com.moataz.motazalqmi and API v1 routes. Do not alter existing article rows, IDs or live admin workflows. Add APIs backwards compatibly. Preserve local bookmarks/history separately from public content. Never substitute Debug signing for Release. Updating an old install requires the same signing certificate.

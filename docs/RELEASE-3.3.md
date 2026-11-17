# Motaz Alalqami 3.3 — استوديو النشر

This release continues the native and web magazine delivered in 3.2. The canonical source is the main branch of Mtzallqmy/Motazalqmi. The current edition-release workflow owns automatic verification and release delivery. Historical Android workflows remain available for manual runs, avoiding duplicate builds and old-version artifact names on new commits. PR #8 contains the implementation milestones.

## Working changes

- Website and native writer panels can schedule publication. Dates entered locally are sent with an explicit timezone as ISO timestamps. A shared PostgreSQL view excludes future publications from public lists, direct article APIs, Android snapshots, SEO documents and the sitemap. No cron worker is required. Cache expiry and the device's next synchronization determine when each reader receives due content.
- Native administration includes a searchable, paginated Blob library and author profile creation/editing, biography, HTTPS links and portrait upload. Existing article author names remain supported. Editing an author retains the same identifier.
- Media deletion checks all articles, drafts, author/site metadata and article revisions. A transaction reserves an unused file before deleting it from the existing Blob store. Database triggers prevent new references to reserved or deleted files. Failed storage deletion remains visible for explicit retry. Used files return a conflict and are retained.
- Telegram code adds scheduled-article management, bounded media attachments at the requested inline position, compare-and-update conflict protection, failed-command inspection, atomic one-time pairing, and confirmation before site-description changes. Configuration checks getMe, setWebhook and getWebhookInfo without returning secrets. Production activation remains pending the rotated credential and account pairing.

## API additions and compatibility

| Contract | Behavior |
| --- | --- |
| Article scheduledAt | Optional ISO timestamp with timezone; empty means immediate publication. Drafts are private. Older published updates that omit this field preserve an existing schedule. |
| GET /api/v1/assets | Authenticated items/cursor plus pendingDeletions. |
| POST /api/v1/media-delete | Authenticated JSON `{url, confirm:true}`; requires a file belonging to the existing store. A reference conflict returns HTTP 409. |
| POST /api/v1/authors | Existing authenticated profile API, now available in native administration. |
| POST /api/v1/telegram-pair | Adds verify; configure verifies identity and webhook. revoke requires confirm:true. |

The existing session, snapshot and Markdown contracts remain in use. This is a native Compose application. Room schema and reader-state storage are unchanged. Large videos are not automatically downloaded.

## Migration and validation

Migration 004 was first validated on the existing isolated Neon verification branch. It was then applied transactionally to Neon main on 17 November 2026. The five production articles and their content digest were unchanged before and after migration. The new public view returned those same five articles. Migration rollback guidance is in EDITORIAL-3.3-MIGRATION.md.

The implementation passed 46 JavaScript tests, the project check and Vite build. Chromium checks cover mobile RTL layout, desktop layout, reader preference persistence, editor recovery, scheduled-content exclusion, and refusal to remove referenced media. Native unit tests and lint passed. Seven instrumentation tests passed on each of API 26, 28 and 29, including explicit media-deletion confirmation and preservation of an edited author's identity. The exact signed ARM64 artifact passed installation and offline launch on an API 35 emulator using ARM translation. GitHub Actions records contain the individual results.

`scripts/verify-editorial33.mjs` exercises the real server handlers with an isolated Neon connection and the existing Blob store. It requires DATABASE_URL, BLOB_READ_WRITE_TOKEN and MOTAZ_VERIFY_ISOLATED=true, generates temporary authentication in memory, and cleans up its own uploaded file and records. Never point a verification run at production. This integration run passed on 17 November 2026: author create/edit, future-publication privacy, due Android snapshot visibility, optimistic conflicts, actual Blob upload, referenced-file deletion rejection, successful unused-file deletion, idempotent retry, prevention of reuse after deletion, and cleanup. Authenticated preview HTTP checks separately confirmed scheduled-content privacy and publication visibility; a later deployment-protection response interrupted the preview media sequence, which was then completed through the real server handlers with the cloud services.

These are emulator checks. No physical-device verification or measured Core Web Vitals improvement is claimed.

## Installation and operational requirements

Android minimum SDK is 26, versionCode 30300, versionName 3.3.0, ABI arm64-v8a. The workflow verifies Android signing, zip alignment, APK ZIP integrity, package identity and SHA256 before publishing from main.

A production update requires the original com.moataz.motazalqmi signing certificate, compared with v2.0.0. When those signing secrets are absent, the independent package is com.moataz.motazalqmi.edition33. It installs alongside earlier apps and does not replace their differently signed installations. Each installation retains its own saved articles and reading history. Keep the older app if you need its local library. Signing recovery is encrypted to the owner's existing recovery certificate.

Telegram production operation is not verified. The previously exposed token must be rotated through BotFather. Store the replacement directly in Vercel's production TELEGRAM_BOT_TOKEN variable, redeploy, verify the webhook in the authenticated dashboard, pair the numeric administrator account and test the commands. Do not paste the replacement token into chat. No bot token belongs in an APK or frontend bundle.

Markdown remains the canonical article format. Server authorization and safe-upload checks remain mandatory. There is no cross-installation reading-state migration in this release.

# Motaz Alalqami 3.2

The release is built from the canonical main branch after PR validation. The website is deployed by the existing Vercel Git integration.

## Delivered changes

- A paper masthead and featured writing lead the native magazine. Sections use different icons, and cards include the article's author identity and portrait fallback.
- The reader offers a table of contents, typography and appearance sheet, author article discovery, and inline audio playback. Audio and video pause when the app enters the background.
- Native search combines Arabic normalization, category filtering and date ordering. Reading history is ordered by most recently opened.
- The website includes a saved-link library, dedicated literature and book shelves, responsive navigation, personal appreciation labels, and a searchable writer dashboard.
- The Markdown editor supports local recovery, revision recovery, conflict rejection, undo and redo, and table/poetry formatting.
- Android download metadata is obtained from published GitHub Releases, including newer prereleases. No pending APK is advertised as downloadable.

## Data and validation

The existing Neon main project and Blob store remain in use. This release adds no production schema migration. Migration 003 retains previous slugs and article revisions.

The live verification script was run against the isolated Neon verification branch. It checked authentication, draft persistence, publication, optimistic conflict rejection, old-slug redirection, SEO HTML, revision history and Android synchronization. It deleted its temporary article afterward.

The web build and 34 JavaScript tests pass locally. The release workflow runs Chromium mobile/desktop and draft recovery checks, native unit tests and lint, API 26/28/29 instrumentation, and installation and offline launch of the exact signed ARM64 release file on API 35 with ARM translation. The GitHub run records are authoritative for completed device checks.

## Installation and signing

Production updates use com.moataz.motazalqmi and require the original production certificate. The workflow compares that certificate against v2.0.0 before distributing a production update.

When the original signing secrets are absent, the optimized Release uses com.moataz.motazalqmi.edition3. It installs alongside previous apps, whose local saved items stay in their respective apps. The signing recovery archive is encrypted to the existing owner recovery certificate.

Physical ARM64 device testing requires an available device. Emulator results are reported as emulator results.

## Operations

Telegram production activation requires a freshly rotated token in TELEGRAM_BOT_TOKEN. Configure TELEGRAM_WEBHOOK_SECRET and pair the administrator from the authenticated website. The token must be rotated through BotFather because an earlier token was exposed. No Telegram operational claim is made until pairing and end-to-end bot tests succeed.

The canonical editorial representation is Markdown. Scheduled publishing, destructive media management and cross-installation reading-state migration remain outside this release.

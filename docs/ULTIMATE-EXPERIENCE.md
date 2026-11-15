# Editorial platform upgrade

Baseline `6fa8122` on main. Production uses Vite on Vercel, Neon main, and the public motazalqmi-media Blob store. The official v2.0.0 Android package and v2.3.0 preview3 have distinct signing and install histories.

## Baseline audit and implementation checklist

- React routes load the editor and Markdown parser on the public landing page. Split the routes and ship local Arabic fonts.
- The landing page places hard-coded decorative content ahead of publications. Lead with an actual article and searchable recent writing.
- Author name and portrait are stored per article. Preserve historical bylines and native DTOs.
- The web editor can reset its form when background synchronization returns. Preserve local recovery state and add conflict checks.
- Article pages share a generic HTML shell with crawlers. Generate article-specific head metadata, a canonical URL, and a sitemap for published work.
- Android uses Compose, Room, WorkManager and a private image cache. Retain saved items across server deletions. Improve discovery and reading continuity from Home.
- Release requires certificate comparison against the official artifact, a release-mode build, ABI inspection, and device checks. The existing standalone workflow signs debug packages with CI debug keys.
- Telegram pairing requires an authenticated website session and numeric IDs. The production environment inventory contained database, authentication and Blob variables. Rotate the exposed bot token before activation.

## Database

The primary database has blog_articles (id primary key, unique slug, status/publication/updated index, JSONB payload), blog_public_metadata (id primary key, JSONB payload) and blog_login_attempts (ip_hash primary key). No foreign-key relationship joins these three records. Preserve the existing HTTP endpoints and public identifiers.

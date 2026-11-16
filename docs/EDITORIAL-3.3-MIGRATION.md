# Editorial 3.3 migration

Apply 004_schedule_media.sql on the verification branch first and then on main before deploying the code. The additive view limits public content to published articles whose optional scheduledAt timestamp is due. Existing articles without that field are visible as before. All public APIs, SEO documents and Android snapshots select the same view. Scheduling is evaluated by PostgreSQL on each refresh, including after hosting downtime. Public caches can delay visibility by their normal TTL, and Android background refresh follows the existing device schedule.

Media deletion reserves an unreferenced Blob URL in a transaction before touching storage. The advisory lock is acquired in a separate transaction statement, so the reference query sees committed writes after lock acquisition. Triggers on article, author/site metadata and revision writes serialize against the same lock and reject references to reserved or deleted files. Article revisions count as references.

A storage failure leaves a pending deletion that the administrator can retry. A completed deletion cannot be reinserted into article content. The UI requires explicit confirmation and the API verifies membership in the existing Blob store.

For rollback, deploy the previous application version while retaining the additive view, reservation table and guards. Do not drop reservations for files already deleted from storage. Published data and identities require no destructive conversion.

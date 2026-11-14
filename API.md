# Motazalqmi API v1 — عقد التكامل الرسمي

Base URL: `https://motazalqmi.vercel.app/api/v1`

واجهة عامة للقراءة فقط. كل البيانات من موقع المدونة وقاعدة Neon الفعلية. بيانات الكاتب والمسودات غير متاحة للعموم.

| HTTP | المسار | البيانات |
|---|---|---|
| GET | `/bootstrap` | `site`, `sections`, `quotes`, أول 100 مقالة منشورة كاملة، و`total`, `hasMore` |
| GET | `/articles?limit=30&offset=0` | قائمة منشورة ملخّصة؛ يمكن استخدام `full=1` للحصول على المحتوى كاملًا |
| GET | `/articles?category=books&q=العلم&featured=1` | ترشيح بالقسم، أو البحث النصي، أو المميزة. جميع المرشحات اختيارية وتُطبّق معًا |
| GET | `/article?slug=article-slug` | مقالة كاملة؛ بديل `id=article-id` |
| GET | `/categories` | عناوين الأقسام ووصفها؛ `about` قسم تعريفي |
| GET | `/quotes` | الاقتباسات ومصادرها ومؤلفوها |
| GET | `/about` | الهوية والحقوق ورسالة المدونة والرابط الرسمي |
| GET | `/health` | التحقق الفعلي من الاتصال بـ Neon دون إظهار أسرار |

جميع استجابات البيانات تحتوي `apiVersion: 1`. صفحات المقالات تحتوي `total`, `limit`, `offset`, `hasMore`. `limit <= 100`. `GET` و`HEAD` فقط؛ `POST` محظور. `ETag` و `If-None-Match` للمزامنة؛ الأخطاء تحمل حقل `error`. ترسل الاستجابات `Content-Type: application/json; charset=utf-8` وقيود التخزين المؤقت.

### مثال

```sh
curl -fsS https://motazalqmi.vercel.app/api/v1/bootstrap
curl -fsS 'https://motazalqmi.vercel.app/api/v1/articles?full=1&limit=100&offset=100'
curl -fsS https://motazalqmi.vercel.app/api/v1/health
```

### حدود التحديث

يتلقى التطبيق نصوص المقالات المنشورة كلها وفق صفحات API، ولا يستدعي Neon مباشرة. بعد نشر أو تعديل مقالة من لوحة الكاتب على الموقع، تحدث مزامنتها في فتح التطبيق أو بالنقر على «تحديث المقالات»؛ يعمل المخزون السابق دون شبكة. **ليست المزامنة الفورية عبر Push notifications**، ولا تتضمن المسودات الخاصة أو بيانات دخول المدير.

| GET | `/android` | معرف التطبيق وإصداره المستهدف، ورابط بناء APK على GitHub، وحالة آخر تشغيل لـAndroid CI إذا توفرت علنًا |
## Premium v2 additions

`GET /api/v1/sync` returns a single authoritative PostgreSQL MVCC snapshot:

```json
{"apiVersion":1,"mode":"snapshot","complete":true,"authoritative":true,"revision":"sha256...","ids":["seed-1"],"total":1,"articles":[{"id":"seed-1","slug":"imru-al-qais-poetic-imagery","status":"published","title":"...","content":"...","updatedAt":"..."}],"sections":[],"quotes":[],"site":{}}
```

The example is abbreviated. Actual production results are recorded in `docs/API-VERIFICATION.json`. `since` accepts an ISO timestamp or revision hash but this implementation sends a full snapshot, not a delta. Clients may use ETag/If-None-Match (304). No timestamp cursor can by itself detect hard deletions; reconcile only against a complete authoritative response. A total of zero is authoritative when `complete:true`.

The response is rejected with 503 rather than truncated above 2000 articles / 3 MiB. Never apply a 503, interrupted, malformed or incomplete snapshot as a deletion list.

Additional routes: `/api/v1/search?q=...`, `/api/v1/featured`, `/api/v1/config`, `/api/v1/versions`, and `/api/v1/articles/{slug}`. Existing `/article?slug=...`, `/articles`, `/bootstrap`, `/categories`, `/quotes`, `/about`, `/android` and `/health` remain supported. Search normalizes Arabic diacritics/alef variants and matches authors/tags/section names. Pagination uses `limit` (1–100) and `offset`.

Sections, quotes and site metadata are read from `blog_public_metadata` by both website and Android API. Apply `db/migrations/002_public_metadata.sql` before deploying. It is idempotent and does not modify article rows or overwrite existing editorial metadata. Existing owner-controlled article administration remains unchanged. No writer login was added to the reader.

Server secrets stay in Vercel. Android connects only over HTTPS to the public API. Vercel manages response compression; ETags and CDN cache headers are set by handlers. API errors retain a string `error` for backward compatibility with additional `apiVersion`/`code` where applicable.

Public handlers also apply bounded per-instance burst protection (120 requests per minute per address, 429 with Retry-After). This is not a globally coordinated quota across serverless instances. Strong distributed abuse protection should be configured at the Vercel perimeter for the actual traffic profile.


## Android 2.1 — images and protected writer access

The published Android reader requests `GET /api/v1/sync` for complete current text and metadata. The new public
`GET /api/v1/media` returns all unique HTTPS image URLs used by published article covers and Markdown body images:

```json
{"apiVersion":1,"media":[{"url":"https://example.org/cover.jpg","kind":"cover","articleId":"seed-1","slug":"example"}],"total":1,"downloadRequired":true}
```

Images are **not** binary-embedded in the JSON. Coil downloads them on Android, with a private persistent offline cache.
Source images are currently hosted at external HTTPS origins and may become unavailable; a permanent upload/storage
solution is a separate capability. The public media list excludes drafts.

Writer access is deliberately separated from public reader data:

1. `POST /api/v1/mobile-admin` with JSON `{"email":"...","password":"..."}`, over HTTPS.
2. The response provides a signed, purpose-bound **two-hour** `accessToken`. Save it in volatile Android memory
   only; never embed it in APK, Git or telemetry.
3. `GET /api/articles?scope=admin` with `Authorization: Bearer <accessToken>` lists drafts and published posts.
4. `POST /api/articles` with the same bearer and JSON `{"action":"save","article":{...}}` or
   `{"action":"delete","id":"..."}` performs writer actions after server authorization.
5. Discard the token at logout; expired sessions require sign-in. The existing browser cookie flow at
   `/api/session` and same-origin protection remain unchanged.

Admin API endpoints must never appear in public, unauthenticated snapshots. Password hashing, rate limiting and
permission enforcement remain on the Vercel server. On the Hobby plan, media and mobile-admin share the existing
`/api/v1/[resource]` dynamic function; do not re-add standalone handlers that exceed the 12-function deployment limit.

Website React/CSS visual components are not serializable through a JSON API. Android implements native
counterparts and shares the site name, tagline, about text, article content, sections, quotes and image references.

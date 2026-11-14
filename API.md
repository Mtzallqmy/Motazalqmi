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

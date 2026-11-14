# مدونة معتز العلقمي | Moataz Alqami Blog

مدونة ثقافية وأدبية باللغة العربية (RTL)، مستوحاة من تصميم المخطوطات العربية وتدرجات التركواز والذهبي، وتحافظ على الأقسام والنصوص الأساسية في النموذج الأصلي.

## المزايا

- ثمانية أقسام: كتابات عامة، اقتباسات، من بطون الكتب، من وحي الأقلام، أحاديث وأخبار، حكم ومواعظ، أدبنا العربي، وحول المدونة.
- الصفحة الرئيسية، المقالات المميزة، اقتباس اليوم، البحث، الوسوم، صور الأغلفة، التنقل المباشر بين الروابط، وعرض Markdown عربي منسق.
- لوحة كاتب محمية بمصادقة Firebase Email/Password، وبوثيقة دور مدير داخل Firestore. يمكن كتابة المقالات وتعديلها وحفظ المسودات وحذفها واستيراد الأمثلة السبعة.
- الوضع التجريبي يعمل دون Firebase للقراءة فقط. **لا يخزن المقالات الجديدة ولا يوفر نشرًا محليًا وهميًا.**
- إعدادات نشر Vercel وFirebase Hosting، واختبار بناء آلي GitHub Actions.

## التقنية والمتطلبات

React 19 + Vite 6 + Tailwind CSS 3 + React Router 7 + Firebase Auth/Firestore + Lucide + React Markdown. يتطلب Node.js 20.19 أو أحدث.

## التشغيل المحلي

```bash
npm install
cp .env.example .env
npm run dev
```

الموقع التجريبي يظهر بالمقالات السبعة المضمنة دون تهيئة Firebase. `npm run check` يفحص وجود الملفات الأساسية، و`npm run build` يصنع النسخة الإنتاجية داخل `dist/`.

## ربط Firebase والمحتوى الدائم

1. أنشئ مشروع Firebase وتطبيق Web، وفعّل **Authentication > Sign-in method > Email/Password**، و**Cloud Firestore**.
2. انسخ بيانات Firebase Web SDK إلى `.env` محليًا، أو أضف مفاتيح `VITE_FIREBASE_*` في إعدادات نشر Vercel. القيم في `.env.example` أمثلة وليست بيانات حقيقية. لا ترفع `.env`.
3. من Firebase Console، أنشئ حساب مستخدم ببريد وكلمة مرور في Authentication، ثم انسخ **UID**.
4. من Firestore Console، أنشئ الوثيقة `admins/USER_UID` وأضف الحقل `enabled` نوع **boolean** وقيمته `true`.
5. انشر قواعد `firestore.rules` **قبل فتح الموقع للعامة**. باستخدام Firebase CLI مع مشروعك الصحيح:

   ```bash
   npx firebase-tools login
   npx firebase-tools use --add
   npx firebase-tools deploy --only firestore:rules
   ```

6. ادخل `/admin/login` بالحساب المعتمد. في قاعدة بيانات خالية يمكنك استيراد الأمثلة السبعة بنقرة واحدة. ستصبح البيانات مشتركة ودائمة عبر Firestore.

> **الأمان:** لا تعتمد الحماية على إخفاء رابط لوحة الكاتب فقط؛ تمنع قواعد Firestore الزوار من الكتابة والقراءة للمسودات، كما تمنع التطبيق من منح نفسه صلاحية المدير. امنح الأدوار يدويًا من Firebase Console. لا تنشر حسابك أو كلمة المرور ضمن المستودع.
>
> **الخصوصية:** الإعجابات والمحفوظات تُخزن محليًا في المتصفح وليست عدادات عامة فعلية. تُعرض الأرقام الموجودة داخل بيانات المقالات للتوافق مع العرض الأصلي. قياس المشاهدات والتفاعل الحقيقي يحتاج خدمة خلفية محمية منفصلة.

## النشر إلى Vercel

- اربط مستودع GitHub بمشروع جديد في Vercel.
- Framework preset: `Vite`.
- Build command: `npm run build`، Output directory: `dist`.
- أضف متغيرات البيئة الستة في إعدادات المشروع، ثم أعد النشر.
- `vercel.json` يضمن وصول روابط المقالات والأقسام إلى React Router عند إعادة تحميل الصفحة.

## بنية المشروع

```text
src/
  components/       Header, Footer, ArticleCard, Ornaments
  context/          AuthContext: جلسة الكاتب ودور المدير
  data/             categories, quotes, articles (أمثلة من النص الأصلي)
  hooks/            ArticlesProvider (Firestore), useBookmarks
  lib/              firebase, format
  pages/            Home, Category, Article, About, Login, Dashboard, NotFound
  styles/           خطوط عربية، ألوان، حركات، تنسيق المقالات
  App.jsx           التنقل والمسارات المحمية
public/             شعار SVG
firestore.rules     قواعد قراءة ونشر آمنة
.github/workflows/ci.yml   فحص وبناء آلي
```

## ملحوظات تتعلق بالمحتوى

تتضمن الأمثلة نصوصًا تراثية ومعلومات منشورة في نموذج أولي. ينبغي مراجعة نسبة الأبيات والأقوال إلى أصحابها والتحقق من حقوق صور Unsplash قبل النشر النهائي. لم يُضف اتصال Firebase حقيقي أو مفاتيح سرية إلى المستودع.

© 2026 معتز العلقمي — تعز، اليمن. جميع الحقوق محفوظة.

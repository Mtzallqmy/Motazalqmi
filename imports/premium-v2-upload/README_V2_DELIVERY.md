# دليل دمج وتشغيل حزمة «مدونة معتز العلقمي — Android Premium v2.0.0»

**برمجة وتطوير:** معتز العلقمي — تعز — اليمن — 2026م.

## خطوات الرفع إلى GitHub وبناء APK الموقّع تلقائيًا

1. أنشئ الفرع الجديد في مستودعك المحلي:
   ```bash
   git checkout -b feat/motazalqami-android-premium-v2
   ```
2. انسخ محتويات هذه الحزمة (`android/` و`api/` و`API.md` و`.github/workflows/android-release.yml`) إلى جذر مستودع `Mtzallqmy/Motazalqmi`.
3. أضف الأسرار التالية في **GitHub Repository Settings → Secrets and variables → Actions** لتوقيع إصدار Release:
   - `ANDROID_KEYSTORE_BASE64` (ملف الـKeystore مشفّرًا بـ Base64)
   - `ANDROID_KEYSTORE_PASSWORD`
   - `ANDROID_KEY_ALIAS`
   - `ANDROID_KEY_PASSWORD`
4. ادفع التغييرات إلى GitHub:
   ```bash
   git add .
   git commit -m "feat(android): rebuild native Kotlin/Compose app v2.0.0 and API v1"
   git push -u origin feat/motazalqami-android-premium-v2
   ```
5. سيقوم GitHub Actions تلقائيًا بتشغيل اختبارات API وAndroid، وبناء ملف `arm64-v8a` Release APK (`com.moataz.motazalqmi` — الإصدار `2.0.0` / `versionCode 200`)، والتحقق من التوقيع، ورفع الحزمة في Artifacts.

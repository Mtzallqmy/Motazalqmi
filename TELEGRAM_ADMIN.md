# بوت الإدارة: مدونة معتز العلقمي

## الأمان والتهيئة

- رابط Webhook: https://motazalqmi.vercel.app/api/v1/telegram
- أنشئ البوت من @BotFather واحفظ Bot Token في Vercel Production فقط تحت اسم TELEGRAM_BOT_TOKEN.
- أضف رقم المستخدم الرقمي لحسابك الخاص إلى TELEGRAM_ADMIN_IDS (يمكن إضافة أرقام مفصولة بفاصلة).
- أنشئ سلسلة عشوائية 32 حرفًا أو أكثر في TELEGRAM_WEBHOOK_SECRET.
- أعد نشر الموقع بعد تعيين المتغيرات.
- من جهازك الذي تُخزّن عليه الأسرار بطريقة آمنة نفّذ تسجيل webhook عبر Telegram Bot API (POST setWebhook) مع url أعلاه و secret_token مساوية إلى TELEGRAM_WEBHOOK_SECRET و allowed_updates تساوي message,callback_query.
- لا تنشر Token أو secret في README أو GitHub أو المحادثات أو APK.

## الأوامر

/help, /articles, /drafts, /stats, /show slug, /link slug
/new العنوان | محتوى المقال (مسودة افتراضيًا)
/title slug | العنوان الجديد
/text slug | متن Markdown
/excerpt slug | الملخص
/cover slug | رابط HTTPS
/category slug | القسم
/featured slug
/publish slug
/unpublish slug
/delete slug (تأكيد بزر مستقل)
/site
/tagline النص
/aboutset النص
/quotes
/quoteadd النص | القائل | المصدر
/quotedelete رقم الاقتباس (تأكيد بزر مستقل)

المقالات الجديدة تُنشأ مسودات لتجنّب النشر العرضي. الحذف يتطلب موافقة منفصلة من الحساب المخوّل. تحقق المنظومة من رقم المستخدم الرقمي ومحادثة خاصة وTelegram webhook header قبل أي وصول لقاعدة البيانات. لا يوجد أمر لاستعراض أسرار Vercel أو كلمات المرور. المحتوى الكبير الأفضل تحريره من لوحة الموقع. إعدادات البوت والأمان تظل عبر Vercel.

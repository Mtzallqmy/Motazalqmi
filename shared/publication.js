export function publicationTime(value) {
 if(value===null||value===undefined||value==='')return '';
 if(typeof value!=='string'||!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(?::\d{2}(?:\.\d{1,3})?)?(?:Z|[+-]\d{2}:\d{2})$/.test(value)||!Number.isFinite(Date.parse(value)))
  throw Error('موعد النشر غير صالح. استخدم موعدًا مع منطقة زمنية.');
 return new Date(value).toISOString();
}
export function isScheduled(article,now=Date.now()) {
 return article.status==='published'&&Boolean(article.scheduledAt)&&Date.parse(article.scheduledAt)>now;
}
export function publicationLabel(article,now=Date.now()) {
 return isScheduled(article,now)?'مجدول':article.status==='draft'?'مسودة':'منشور';
}

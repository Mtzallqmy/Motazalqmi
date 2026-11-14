import { neon } from '@neondatabase/serverless';

export function database() {
  const url = process.env.DATABASE_URL;
  if (!url || !/^postgres(ql)?:\/\//.test(url)) throw new Error('DATABASE_URL missing');
  return neon(url);
}
export function articleRow(row) { return { ...row.data, id: row.id, slug: row.slug, status: row.status, publishDate: row.publish_date }; }
export function normalizeArticle(a) {
  if (!a || typeof a !== 'object' || Array.isArray(a)) throw new Error('بيانات المقالة غير صالحة.');
  const str = (name, limit, fallback = '') => {
    const value = a[name] == null ? fallback : a[name];
    if (typeof value !== 'string' || value.length > limit) throw new Error('حقل غير صالح: ' + name);
    return value.trim();
  };
  const title = str('title', 220), content = str('content', 180000), slug = str('slug', 150), id = str('id', 150);
  if (!title || !content || !/^[\p{L}\p{N}-]+$/u.test(slug) || !slug) throw new Error('العنوان والمحتوى والرابط الوصفي مطلوبة.');
  const status = a.status === 'draft' ? 'draft' : a.status === 'published' ? 'published' : null;
  if (!status) throw new Error('حالة المقالة غير صالحة.');
  const date = str('publishDate', 10, new Date().toISOString().slice(0,10));
  if (!/^\d{4}-\d{2}-\d{2}$/.test(date) || Number.isNaN(Date.parse(date))) throw new Error('تاريخ المقالة غير صالح.');
  const tags = Array.isArray(a.tags) ? a.tags.filter(x => typeof x === 'string').slice(0, 20).map(x=>x.slice(0, 55)) : [];
  const coverImage = str('coverImage', 2048);
  if (coverImage && !/^https:\/\//i.test(coverImage)) throw new Error('رابط صورة الغلاف يجب أن يكون HTTPS.');
  return { id, slug, title, content, status, publishDate: date, category: str('category',60,'general'), excerpt:str('excerpt',1000), coverImage, author:str('author',150,'معتز العلقمي'), readTime:str('readTime',100,'5 دقائق'), featured:a.featured===true, tags, views:Number.isSafeInteger(a.views)&&a.views>=0?a.views:0, likes:Number.isSafeInteger(a.likes)&&a.likes>=0?a.likes:0 };
}

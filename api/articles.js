import { randomUUID } from 'node:crypto';
import { checkOrigin, getAdmin, methodNotAllowed, privateResponse } from '../server/auth.js';
import { articleRow, database, normalizeArticle } from '../server/db.js';

export default async function handler(req, res) {
  privateResponse(res);
  if (req.method !== 'GET' && req.method !== 'POST') return methodNotAllowed(res, 'GET, POST');
  const admin = getAdmin(req);
  if (req.method === 'POST' && (!admin || !checkOrigin(req))) return res.status(403).json({ error: 'غير مصرح بإجراء هذا التعديل.' });
  if (req.method === 'GET' && req.query.scope === 'admin' && !admin) return res.status(401).json({ error: 'تسجيل الدخول مطلوب.' });
  try {
    const sql = database();
    if (req.method === 'GET') {
      const rows = req.query.scope === 'admin'
        ? await sql`SELECT id, slug, status, publish_date::text, data FROM blog_articles ORDER BY publish_date DESC, updated_at DESC LIMIT 500`
        : await sql`SELECT id, slug, status, publish_date::text, data FROM blog_articles WHERE status='published' ORDER BY publish_date DESC, updated_at DESC LIMIT 500`;
      res.setHeader('Cache-Control', 'no-store');
      return res.status(200).json({ articles: rows.map(articleRow) });
    }
    const action = req.body?.action;
    if (JSON.stringify(req.body || '').length > 240000) return res.status(413).json({ error: 'البيانات تتجاوز الحد المسموح.' });
    if (action === 'save') {
      const a = normalizeArticle(req.body.article);
      const id = a.id || `art-${randomUUID()}`;
      const data = JSON.stringify({...a, id});
      await sql`INSERT INTO blog_articles (id, slug, status, publish_date, data) VALUES (${id}, ${a.slug}, ${a.status}, ${a.publishDate}::date, ${data}::jsonb)
        ON CONFLICT (id) DO UPDATE SET slug=EXCLUDED.slug, status=EXCLUDED.status, publish_date=EXCLUDED.publish_date, data=EXCLUDED.data, updated_at=NOW()`;
      return res.status(200).json({ id });
    }
    if (action === 'delete') {
      if (typeof req.body.id !== 'string' || req.body.id.length > 150) return res.status(400).json({ error: 'معرف المقالة غير صالح.' });
      await sql`DELETE FROM blog_articles WHERE id=${req.body.id}`;
      return res.status(200).json({ ok:true });
    }
    if (action === 'import') {
      const count = await sql`SELECT COUNT(*)::int AS total FROM blog_articles`;
      if (count[0].total > 0) return res.status(409).json({ error: 'قاعدة البيانات تحتوي على مقالات بالفعل.' });
      if (!Array.isArray(req.body.articles) || req.body.articles.length > 7) return res.status(400).json({ error: 'قائمة الاستيراد غير صالحة.' });
      for (const source of req.body.articles) {
        const a = normalizeArticle(source);
        if (!a.id) throw new Error('مقالات الاستيراد تتطلب معرفًا.');
        const data = JSON.stringify(a);
        await sql`INSERT INTO blog_articles (id, slug, status, publish_date, data) VALUES (${a.id}, ${a.slug}, ${a.status}, ${a.publishDate}::date, ${data}::jsonb) ON CONFLICT (id) DO NOTHING`;
      }
      return res.status(200).json({ imported: req.body.articles.length });
    }
    return res.status(400).json({ error: 'أمر غير معروف.' });
  } catch(error) {
    if (error?.code === '23505') return res.status(409).json({ error: 'الرابط الوصفي مستخدم في مقالة أخرى.' });
    if (error?.message?.startsWith('حقل') || /غير صالحة|مطلوبة|يجب/.test(error?.message || '')) return res.status(400).json({ error: error.message });
    console.error('Blog articles request failed', error?.code || error?.name || 'unexpected');
    return res.status(503).json({ error: 'تعذر الوصول إلى قاعدة البيانات. حاول مجددًا.' });
  }
}

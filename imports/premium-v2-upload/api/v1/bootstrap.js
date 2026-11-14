import { getDb } from './_lib/db.js';
import { sendJson, sendError } from './_lib/http.js';

export default async function handler(req, res) {
  try {
    const sql = getDb();
    const [categories, featuredRows, latestArticles, quotes] = await Promise.all([
      sql`SELECT id, slug, name_ar, description, sort_order FROM categories ORDER BY sort_order ASC`,
      sql`SELECT * FROM articles WHERE status = 'published' AND deleted_at IS NULL AND is_featured = true ORDER BY published_at DESC LIMIT 1`,
      sql`SELECT * FROM articles WHERE status = 'published' AND deleted_at IS NULL ORDER BY published_at DESC LIMIT 20`,
      sql`SELECT id, text, author, source, category_id, updated_at FROM quotes WHERE status = 'published' AND deleted_at IS NULL ORDER BY updated_at DESC LIMIT 30`
    ]);

    return sendJson(req, res, {
      data: {
        categories,
        featured: featuredRows[0] || null,
        latestArticles,
        quotes,
        serverTime: new Date().toISOString()
      },
      cacheSeconds: 60
    });
  } catch (err) {
    return sendError(req, res, 500, 'BOOTSTRAP_ERROR', 'Failed to bootstrap mobile client');
  }
}

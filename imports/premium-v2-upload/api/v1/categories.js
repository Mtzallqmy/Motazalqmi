import { getDb } from './_lib/db.js';
import { sendJson, sendError } from './_lib/http.js';

export default async function handler(req, res) {
  try {
    const sql = getDb();
    const rows = await sql`
      SELECT c.id, c.slug, c.name_ar, c.description, c.sort_order,
             COUNT(a.id)::int AS article_count
      FROM categories c
      LEFT JOIN articles a ON a.category_id = c.id AND a.status = 'published' AND a.deleted_at IS NULL
      GROUP BY c.id
      ORDER BY c.sort_order ASC
    `;
    return sendJson(req, res, { data: rows, cacheSeconds: 300 });
  } catch (err) {
    return sendError(req, res, 500, 'CATEGORIES_ERROR', 'Failed to load categories');
  }
}

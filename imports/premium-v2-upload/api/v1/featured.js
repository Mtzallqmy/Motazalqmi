import { getDb } from './_lib/db.js';
import { sendJson, sendError } from './_lib/http.js';

export default async function handler(req, res) {
  try {
    const sql = getDb();
    const rows = await sql`
      SELECT id, slug, title, excerpt, content_markdown, cover_image_url, author_name,
             category_id, category_name, tags, reading_time_minutes, published_at, updated_at, is_featured
      FROM articles
      WHERE status = 'published' AND deleted_at IS NULL AND is_featured = true
      ORDER BY published_at DESC
      LIMIT 1
    `;
    return sendJson(req, res, { data: rows[0] || null, cacheSeconds: 120 });
  } catch (err) {
    return sendError(req, res, 500, 'FEATURED_ERROR', 'Failed to fetch featured article');
  }
}

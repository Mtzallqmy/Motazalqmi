import { getDb } from './_lib/db.js';
import { sendJson, sendError } from './_lib/http.js';

export default async function handler(req, res) {
  const q = String(req.query?.q || '').trim();
  if (q.length < 2) return sendError(req, res, 400, 'QUERY_TOO_SHORT', 'Search query must be at least 2 characters');

  try {
    const sql = getDb();
    const pattern = `%${q}%`;
    const rows = await sql`
      SELECT id, slug, title, excerpt, content_markdown, cover_image_url, author_name,
             category_id, category_name, tags, reading_time_minutes, published_at, updated_at, is_featured
      FROM articles
      WHERE status = 'published' AND deleted_at IS NULL
        AND (title ILIKE ${pattern} OR excerpt ILIKE ${pattern} OR content_markdown ILIKE ${pattern} OR author_name ILIKE ${pattern})
      ORDER BY published_at DESC
      LIMIT 30
    `;
    return sendJson(req, res, { data: rows, cacheSeconds: 30 });
  } catch (err) {
    return sendError(req, res, 500, 'SEARCH_ERROR', 'Search failed');
  }
}

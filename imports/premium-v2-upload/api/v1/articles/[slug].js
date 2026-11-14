import { getDb } from '../_lib/db.js';
import { sendJson, sendError } from '../_lib/http.js';

export default async function handler(req, res) {
  const slug = String(req.query?.slug || '').trim();
  if (!slug) return sendError(req, res, 400, 'INVALID_SLUG', 'Article slug is required');

  try {
    const sql = getDb();
    const rows = await sql`
      SELECT id, slug, title, excerpt, content_markdown, cover_image_url, author_name,
             category_id, category_name, tags, reading_time_minutes, published_at, updated_at, is_featured
      FROM articles
      WHERE slug = ${slug} AND status = 'published' AND deleted_at IS NULL
      LIMIT 1
    `;
    if (!rows.length) return sendError(req, res, 404, 'NOT_FOUND', 'Article not found');
    return sendJson(req, res, { data: rows[0], cacheSeconds: 120 });
  } catch (err) {
    return sendError(req, res, 500, 'ARTICLE_FETCH_ERROR', 'Failed to fetch article');
  }
}

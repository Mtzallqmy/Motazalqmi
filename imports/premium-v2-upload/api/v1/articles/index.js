import { getDb } from '../_lib/db.js';
import { sendJson, sendError, parsePagination } from '../_lib/http.js';

export default async function handler(req, res) {
  try {
    const sql = getDb();
    const { page, limit, offset } = parsePagination(req.query);
    const category = req.query?.category || null;

    const rows = category
      ? await sql`
          SELECT id, slug, title, excerpt, content_markdown, cover_image_url, author_name,
                 category_id, category_name, tags, reading_time_minutes, published_at, updated_at, is_featured
          FROM articles
          WHERE status = 'published' AND deleted_at IS NULL AND category_id = ${category}
          ORDER BY published_at DESC
          LIMIT ${limit} OFFSET ${offset}
        `
      : await sql`
          SELECT id, slug, title, excerpt, content_markdown, cover_image_url, author_name,
                 category_id, category_name, tags, reading_time_minutes, published_at, updated_at, is_featured
          FROM articles
          WHERE status = 'published' AND deleted_at IS NULL
          ORDER BY published_at DESC
          LIMIT ${limit} OFFSET ${offset}
        `;

    return sendJson(req, res, {
      data: rows,
      meta: { page, limit, hasMore: rows.length === limit },
      cacheSeconds: 60
    });
  } catch (err) {
    return sendError(req, res, 500, 'ARTICLES_ERROR', 'Failed to fetch published articles');
  }
}

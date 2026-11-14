import { getDb } from './_lib/db.js';
import { sendJson, sendError, parsePagination } from './_lib/http.js';

export default async function handler(req, res) {
  try {
    const sql = getDb();
    const { page, limit, offset } = parsePagination(req.query);
    const rows = await sql`
      SELECT id, text, author, source, category_id, updated_at
      FROM quotes
      WHERE status = 'published' AND deleted_at IS NULL
      ORDER BY updated_at DESC
      LIMIT ${limit} OFFSET ${offset}
    `;
    return sendJson(req, res, { data: rows, meta: { page, limit, hasMore: rows.length === limit }, cacheSeconds: 120 });
  } catch (err) {
    return sendError(req, res, 500, 'QUOTES_ERROR', 'Failed to fetch quotes');
  }
}
